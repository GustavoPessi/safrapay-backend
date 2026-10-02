# SafraPay - Backend

Backend Spring Boot do projeto SafraPay (TCC - Gustavo Borges Pessi).

## Stack
- Java 17 (Amazon Corretto)
- Spring Boot 3.3.5 (Web, Data JPA, Validation, Actuator)
- PostgreSQL (Amazon RDS)

## Rodando localmente
Defina as variaveis de ambiente antes de rodar (nunca commitar senha real):

```
DB_URL=jdbc:postgresql://<host>:5432/<database>
DB_USERNAME=postgres
DB_PASSWORD=<senha>
```

```
mvn spring-boot:run
```

## Endpoints de teste
- `GET /api/status` - status simples da aplicacao
- `GET /actuator/health` - health check, inclui teste de conexao com o banco

## Convencoes de codigo (01/10/2026)
- **Idioma:** nomes do dominio em portugues (entidades, atributos, metodos,
  variaveis, DTOs): `Safra`, `valorAlvo`, `sincronizarPush`, `converterData`.
- **Fica em ingles so o que e termo tecnico/framework:** sufixos de papel do
  Spring (`Controller`, `Service`, `Repository`, `Request`, `Response`,
  `Config`, `Filter`, `Client`, `Util`), metodos sobrescritos do framework
  (`loadUserByUsername`, `doFilterInternal`), nomes de `@Bean` do Spring
  Security e termos sem traducao usual no protocolo (token, refresh token,
  hash, JWT, login/logout, sync/pull/push, uuid, email, status).
- **Nao misturar** os dois idiomas fora dessas excecoes (ex.: `RegistrarRequest`,
  nao `RegisterRequest`; `converterData`, nao `parseData`).
- **Nomes que vao para o JSON ou para o banco nao se renomeiam por padronizacao**
  (contrato com o app e com o schema): atributos de DTO, colunas, valores de
  enum gravados (ex.: `TipoToken.RESET_SENHA`), chaves do `application.properties`.
- **Encapsulamento:** atributos sempre `private` (getters/setters ou Lombok);
  objetos de valor imutaveis como `record`.
