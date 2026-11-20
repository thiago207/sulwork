UPDATE contribuicao c SET ativo = FALSE
FROM participacao pa
JOIN participante p ON p.id = pa.participante_id
WHERE c.participacao_id = pa.id AND c.ativo AND (NOT p.ativo OR NOT pa.ativo);

UPDATE participacao pa SET ativo = FALSE
FROM participante p
WHERE pa.participante_id = p.id AND pa.ativo AND NOT p.ativo;
