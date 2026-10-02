package com.safrapay.backend.chat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Converte o arquivo que o produtor anexou no chat (planilha .xlsx, .csv ou
 * bloco de notas .txt) em texto simples pra IA ler. Sem biblioteca externa
 * (Apache POI é pesado e não traz nada que precisemos aqui).
 *
 * Segurança, já que o arquivo vem do usuário:
 *  - XML lido com DTD/entidades externas desligados (evita XXE);
 *  - limite de bytes descompactados e de entradas no .zip (evita "zip bomb");
 *  - limite de linhas e de caracteres no texto final (controle de custo da IA).
 */
public final class LeitorArquivo {

    public static final int MAX_LINHAS = 300;
    public static final int MAX_CARACTERES = 40_000;
    private static final long MAX_DESCOMPACTADO = 30L * 1024 * 1024;
    private static final int MAX_ENTRADAS_ZIP = 2_000;
    private static final int MAX_ABAS = 5;

    /**
     * Resultado da leitura (imutável: record = atributos private final + acessores).
     *
     * @param formato     "planilha Excel", "CSV" ou "texto"
     * @param texto       conteúdo em texto, uma linha por registro
     * @param linhasLidas quantas linhas foram lidas
     * @param truncado    true se parou no limite de linhas/caracteres
     */
    public record Conteudo(String formato, String texto, int linhasLidas, boolean truncado) {}

    /** Arquivo em formato que não sabemos ler (vira 415 no service). */
    public static final class FormatoNaoSuportado extends Exception {
        public FormatoNaoSuportado(String msg) { super(msg); }
    }

    private LeitorArquivo() {}

    public static Conteudo ler(String nome, byte[] bytes) throws FormatoNaoSuportado, IOException {
        String n = nome == null ? "" : nome.toLowerCase(Locale.ROOT).trim();
        if (comeca(bytes, 0x50, 0x4B, 0x03, 0x04)) { // PK.. = zip
            if (n.endsWith(".xlsx") || n.endsWith(".xlsm") || n.isEmpty() || !n.contains(".")) return lerXlsx(bytes);
            throw new FormatoNaoSuportado("Arquivo compactado não suportado (use .xlsx, .csv ou .txt)");
        }
        if (comeca(bytes, 0xD0, 0xCF, 0x11, 0xE0)) {
            throw new FormatoNaoSuportado("Planilha .xls antiga: salve como .xlsx ou .csv e envie de novo");
        }
        if (comeca(bytes, 0x25, 0x50, 0x44, 0x46)) {
            throw new FormatoNaoSuportado("PDF ainda não é suportado (envie foto, .xlsx, .csv ou .txt)");
        }
        if (pareceBinario(bytes)) {
            throw new FormatoNaoSuportado("Formato de arquivo não suportado (use .xlsx, .csv ou .txt)");
        }
        String texto = decodificarTexto(bytes);
        String formato = n.endsWith(".csv") ? "CSV" : "texto";
        return limitarLinhas(formato, texto.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1));
    }

    // ------------------------------------------------------------------
    // Texto (.csv / .txt)
    // ------------------------------------------------------------------

    /** UTF-8 se for válido; senão Windows-1252 (o "ANSI" do Excel/Bloco de Notas em português). */
    static String decodificarTexto(byte[] bytes) {
        int ini = comeca(bytes, 0xEF, 0xBB, 0xBF) ? 3 : 0; // BOM UTF-8
        ByteBuffer buf = ByteBuffer.wrap(bytes, ini, bytes.length - ini);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buf).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, ini, bytes.length - ini, Charset.forName("windows-1252"));
        }
    }

    private static Conteudo limitarLinhas(String formato, String[] linhas) {
        StringBuilder sb = new StringBuilder();
        int lidas = 0;
        boolean truncado = false;
        for (String linha : linhas) {
            String l = linha.strip();
            if (l.isEmpty()) continue;
            if (lidas >= MAX_LINHAS || sb.length() + l.length() + 1 > MAX_CARACTERES) {
                truncado = true;
                break;
            }
            sb.append(l).append('\n');
            lidas++;
        }
        return new Conteudo(formato, sb.toString(), lidas, truncado);
    }

    // ------------------------------------------------------------------
    // Planilha (.xlsx)
    // ------------------------------------------------------------------

    private static Conteudo lerXlsx(byte[] bytes) throws IOException, FormatoNaoSuportado {
        Map<String, byte[]> partes = descompactar(bytes);
        byte[] workbook = partes.get("xl/workbook.xml");
        if (workbook == null) throw new FormatoNaoSuportado("Arquivo .xlsx inválido");

        List<String> compartilhadas = textosCompartilhados(partes.get("xl/sharedStrings.xml"));
        Set<Integer> estilosData = estilosDeData(partes.get("xl/styles.xml"));
        Map<String, String> alvos = relacoes(partes.get("xl/_rels/workbook.xml.rels"));

        // abas na ordem do arquivo: nome -> caminho do XML
        Map<String, String> abas = new LinkedHashMap<>();
        NodeList sheets = lerXml(workbook).getElementsByTagNameNS("*", "sheet");
        for (int i = 0; i < sheets.getLength(); i++) {
            Element s = (Element) sheets.item(i);
            String rid = s.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id");
            String alvo = alvos.get(rid);
            if (alvo == null) continue;
            String caminho = alvo.startsWith("/") ? alvo.substring(1) : "xl/" + alvo;
            abas.put(s.getAttribute("name"), caminho);
        }
        // Se existir uma aba de lançamentos (ex.: a planilha exportada pelo próprio app), lê só ela.
        Map<String, String> escolhidas = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : abas.entrySet()) {
            String nomeAba = e.getKey().toLowerCase(Locale.ROOT);
            if (nomeAba.contains("lanç") || nomeAba.contains("lanc")) escolhidas.put(e.getKey(), e.getValue());
        }
        if (escolhidas.isEmpty()) escolhidas = abas;

        StringBuilder sb = new StringBuilder();
        int lidas = 0;
        boolean truncado = false;
        int nAbas = 0;
        for (Map.Entry<String, String> aba : escolhidas.entrySet()) {
            if (nAbas++ >= MAX_ABAS) { truncado = true; break; }
            byte[] xml = partes.get(aba.getValue());
            if (xml == null) continue;
            List<String> linhas = linhasDaAba(xml, compartilhadas, estilosData);
            if (linhas.isEmpty()) continue;
            sb.append("Aba: ").append(aba.getKey()).append('\n');
            for (String l : linhas) {
                if (lidas >= MAX_LINHAS || sb.length() + l.length() + 1 > MAX_CARACTERES) {
                    truncado = true;
                    break;
                }
                sb.append(l).append('\n');
                lidas++;
            }
            if (truncado) break;
        }
        return new Conteudo("planilha Excel", sb.toString(), lidas, truncado);
    }

    private static List<String> linhasDaAba(byte[] xml, List<String> compartilhadas, Set<Integer> estilosData)
            throws IOException {
        List<String> saida = new ArrayList<>();
        NodeList rows = lerXml(xml).getElementsByTagNameNS("*", "row");
        for (int i = 0; i < rows.getLength(); i++) {
            Element row = (Element) rows.item(i);
            TreeMap<Integer, String> celulas = new TreeMap<>();
            NodeList cs = row.getElementsByTagNameNS("*", "c");
            int proxima = 0;
            for (int j = 0; j < cs.getLength(); j++) {
                Element c = (Element) cs.item(j);
                int col = c.hasAttribute("r") ? coluna(c.getAttribute("r")) : proxima;
                proxima = col + 1;
                String v = valorCelula(c, compartilhadas, estilosData);
                if (v != null && !v.isBlank()) celulas.put(col, v.strip().replace('\n', ' '));
            }
            if (celulas.isEmpty()) continue;
            StringBuilder l = new StringBuilder("Linha ").append(row.hasAttribute("r") ? row.getAttribute("r") : String.valueOf(i + 1)).append(": ");
            int ultima = celulas.lastKey();
            for (int k = 0; k <= ultima; k++) {
                if (k > 0) l.append(" | ");
                String v = celulas.get(k);
                if (v != null) l.append(v);
            }
            saida.add(l.toString());
        }
        return saida;
    }

    private static String valorCelula(Element c, List<String> compartilhadas, Set<Integer> estilosData) {
        String t = c.getAttribute("t");
        if ("inlineStr".equals(t)) return textoDe(c);
        String v = filhoTexto(c, "v");
        if (v == null) return null;
        switch (t) {
            case "s":
                try {
                    int idx = Integer.parseInt(v.trim());
                    return idx >= 0 && idx < compartilhadas.size() ? compartilhadas.get(idx) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            case "b":
                return "1".equals(v.trim()) ? "VERDADEIRO" : "FALSO";
            case "str":
            case "e":
                return v;
            default: // número (ou data, dependendo do estilo)
                try {
                    double d = Double.parseDouble(v.trim());
                    int estilo = c.hasAttribute("s") ? Integer.parseInt(c.getAttribute("s")) : 0;
                    if (estilosData.contains(estilo) && d > 0 && d < 2_958_466) {
                        return LocalDate.of(1899, 12, 30).plusDays((long) Math.floor(d)).toString();
                    }
                    return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
                } catch (NumberFormatException e) {
                    return v;
                }
        }
    }

    /** Textos de sharedStrings.xml (cada <si> pode ter vários <t> em rich text). */
    private static List<String> textosCompartilhados(byte[] xml) throws IOException {
        List<String> lista = new ArrayList<>();
        if (xml == null) return lista;
        NodeList sis = lerXml(xml).getElementsByTagNameNS("*", "si");
        for (int i = 0; i < sis.getLength(); i++) lista.add(textoDe((Element) sis.item(i)));
        return lista;
    }

    /** Índices de cellXfs cujo formato numérico é de data. */
    private static Set<Integer> estilosDeData(byte[] xml) throws IOException {
        Set<Integer> datas = new HashSet<>();
        if (xml == null) return datas;
        Document doc = lerXml(xml);
        Map<Integer, String> formatos = new HashMap<>();
        NodeList nfs = doc.getElementsByTagNameNS("*", "numFmt");
        for (int i = 0; i < nfs.getLength(); i++) {
            Element nf = (Element) nfs.item(i);
            try {
                formatos.put(Integer.parseInt(nf.getAttribute("numFmtId")), nf.getAttribute("formatCode"));
            } catch (NumberFormatException ignored) { }
        }
        NodeList xfsPai = doc.getElementsByTagNameNS("*", "cellXfs");
        if (xfsPai.getLength() == 0) return datas;
        NodeList xfs = ((Element) xfsPai.item(0)).getElementsByTagNameNS("*", "xf");
        for (int i = 0; i < xfs.getLength(); i++) {
            Element xf = (Element) xfs.item(i);
            int id;
            try { id = Integer.parseInt(xf.getAttribute("numFmtId")); } catch (NumberFormatException e) { continue; }
            if ((id >= 14 && id <= 22) || (id >= 45 && id <= 47) || ehFormatoData(formatos.get(id))) datas.add(i);
        }
        return datas;
    }

    static boolean ehFormatoData(String codigo) {
        if (codigo == null) return false;
        String limpo = codigo.replaceAll("\"[^\"]*\"", "").replaceAll("\\[[^\\]]*\\]", "").toLowerCase(Locale.ROOT);
        return limpo.contains("d") || limpo.contains("y"); // o .xlsx guarda os códigos em inglês (dd/mm/yyyy) mesmo no Excel em português
    }

    private static Map<String, String> relacoes(byte[] xml) throws IOException {
        Map<String, String> mapa = new HashMap<>();
        if (xml == null) return mapa;
        NodeList rels = lerXml(xml).getElementsByTagNameNS("*", "Relationship");
        for (int i = 0; i < rels.getLength(); i++) {
            Element r = (Element) rels.item(i);
            mapa.put(r.getAttribute("Id"), r.getAttribute("Target"));
        }
        return mapa;
    }

    // ------------------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------------------

    private static Map<String, byte[]> descompactar(byte[] bytes) throws IOException, FormatoNaoSuportado {
        Map<String, byte[]> partes = new HashMap<>();
        long total = 0;
        int entradas = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = zip.getNextEntry()) != null) {
                if (++entradas > MAX_ENTRADAS_ZIP) throw new FormatoNaoSuportado("Arquivo .xlsx grande demais");
                String nome = e.getName();
                boolean interessa = nome.equals("xl/workbook.xml") || nome.equals("xl/sharedStrings.xml")
                        || nome.equals("xl/styles.xml") || nome.equals("xl/_rels/workbook.xml.rels")
                        || (nome.startsWith("xl/worksheets/") && nome.endsWith(".xml"));
                if (!interessa || e.isDirectory()) continue;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int n;
                while ((n = zip.read(buf)) > 0) {
                    total += n;
                    if (total > MAX_DESCOMPACTADO) throw new FormatoNaoSuportado("Arquivo .xlsx grande demais");
                    out.write(buf, 0, n);
                }
                partes.put(nome, out.toByteArray());
            }
        } catch (java.util.zip.ZipException ex) {
            throw new FormatoNaoSuportado("Arquivo .xlsx corrompido");
        }
        return partes;
    }

    private static Document lerXml(byte[] xml) throws IOException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                // não imprimir "[Fatal Error]" no log a cada planilha inválida — o erro vira exceção normal
                @Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
                @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
            });
            try (InputStream in = new ByteArrayInputStream(xml)) {
                return b.parse(in);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("XML inválido na planilha", e);
        }
    }

    /** Concatena o texto de todos os <t> descendentes. */
    private static String textoDe(Element e) {
        StringBuilder sb = new StringBuilder();
        NodeList ts = e.getElementsByTagNameNS("*", "t");
        for (int i = 0; i < ts.getLength(); i++) sb.append(ts.item(i).getTextContent());
        return sb.toString();
    }

    private static String filhoTexto(Element e, String tag) {
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && tag.equals(n.getLocalName())) return n.getTextContent();
        }
        return null;
    }

    /** "AB12" -> 27 (índice da coluna, começando em 0). */
    static int coluna(String ref) {
        int col = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = Character.toUpperCase(ref.charAt(i));
            if (ch < 'A' || ch > 'Z') break;
            col = col * 26 + (ch - 'A' + 1);
        }
        return Math.max(0, col - 1);
    }

    private static boolean comeca(byte[] b, int... assinatura) {
        if (b.length < assinatura.length) return false;
        for (int i = 0; i < assinatura.length; i++) if ((b[i] & 0xFF) != assinatura[i]) return false;
        return true;
    }

    private static boolean pareceBinario(byte[] b) {
        int n = Math.min(b.length, 4096);
        for (int i = 0; i < n; i++) if (b[i] == 0) return true;
        return false;
    }
}
