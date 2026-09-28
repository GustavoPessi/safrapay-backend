-- =============================================================================
-- V3 - Alinha Maquina e AnotacaoMaquina com o app (28/09/2026, Fase 4 do sync)
--
-- O app ja tinha estes campos desde a feature de Maquinas, mas o backend foi
-- criado antes e nunca recebeu:
--   - maquina.modelo                 (ex.: "MF 4275") - opcional
--   - anotacao_maquina.concluida     (pendente/concluida) - o app ja marca
--   - anotacao_maquina.data opcional (no app a data eh opcional; no banco
--                                     estava NOT NULL)
-- =============================================================================

ALTER TABLE public.maquina ADD COLUMN modelo character varying(255);

ALTER TABLE public.anotacao_maquina ALTER COLUMN data DROP NOT NULL;
ALTER TABLE public.anotacao_maquina ADD COLUMN concluida boolean NOT NULL DEFAULT false;
