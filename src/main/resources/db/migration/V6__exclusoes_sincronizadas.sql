-- =============================================================================
-- V6 - Exclusoes sincronizadas (08/10/2026). Ver ExclusaoService e o contrato no
--      Notion ("Exclusoes sincronizadas").
--
-- Excluir no app agora vira um aviso pro servidor, que marca excluido_em (sem
-- apagar a linha) no item e, em cascata, nos filhos. Item com excluido_em some
-- das listas e do pull normal e e informado em GET /api/exclusoes/sync pros
-- outros aparelhos apagarem tambem.
-- =============================================================================

ALTER TABLE public.propriedade      ADD COLUMN excluido_em timestamp(6) with time zone;
ALTER TABLE public.talhao           ADD COLUMN excluido_em timestamp(6) with time zone;
ALTER TABLE public.safra            ADD COLUMN excluido_em timestamp(6) with time zone;
ALTER TABLE public.receita          ADD COLUMN excluido_em timestamp(6) with time zone;
ALTER TABLE public.despesa          ADD COLUMN excluido_em timestamp(6) with time zone;
ALTER TABLE public.meta_financeira  ADD COLUMN excluido_em timestamp(6) with time zone;
ALTER TABLE public.maquina          ADD COLUMN excluido_em timestamp(6) with time zone;
ALTER TABLE public.anotacao_maquina ADD COLUMN excluido_em timestamp(6) with time zone;

-- Os avisos de exclusao sao poucos: indices parciais so com as linhas excluidas.
CREATE INDEX idx_propriedade_excluido      ON public.propriedade (excluido_em)      WHERE excluido_em IS NOT NULL;
CREATE INDEX idx_talhao_excluido           ON public.talhao (excluido_em)           WHERE excluido_em IS NOT NULL;
CREATE INDEX idx_safra_excluido            ON public.safra (excluido_em)            WHERE excluido_em IS NOT NULL;
CREATE INDEX idx_receita_excluido          ON public.receita (excluido_em)          WHERE excluido_em IS NOT NULL;
CREATE INDEX idx_despesa_excluido          ON public.despesa (excluido_em)          WHERE excluido_em IS NOT NULL;
CREATE INDEX idx_meta_financeira_excluido  ON public.meta_financeira (excluido_em)  WHERE excluido_em IS NOT NULL;
CREATE INDEX idx_maquina_excluido          ON public.maquina (excluido_em)          WHERE excluido_em IS NOT NULL;
CREATE INDEX idx_anotacao_maquina_excluido ON public.anotacao_maquina (excluido_em) WHERE excluido_em IS NOT NULL;
