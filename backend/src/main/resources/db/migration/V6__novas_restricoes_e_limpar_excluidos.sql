-- novas restrições: alergia a peixe e sem mel
ALTER TABLE participante_restricao DROP CONSTRAINT ck_participante_restricao;
ALTER TABLE participante_restricao ADD CONSTRAINT ck_participante_restricao CHECK (restricao IN (
    'VEGANO', 'VEGETARIANO', 'SEM_GLUTEN', 'SEM_LACTOSE', 'SEM_MEL',
    'ALERGIA_AMENDOIM', 'ALERGIA_FRUTOS_DO_MAR', 'ALERGIA_OVO', 'ALERGIA_PEIXE'));

-- antes, excluir um participante não mexia nos eventos dele; aqui essas
-- participações e contribuições deixam de contar (exclusão lógica, o registro fica)
UPDATE contribuicao SET ativo = FALSE
WHERE ativo AND participacao_id IN (
    SELECT pa.id FROM participacao pa
    JOIN participante p ON p.id = pa.participante_id
    WHERE NOT p.ativo);

UPDATE participacao SET ativo = FALSE
WHERE ativo AND participante_id IN (SELECT id FROM participante WHERE NOT ativo);
