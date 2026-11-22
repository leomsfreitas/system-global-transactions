# 0010: Arredondamento e casas decimais por moeda

## Decisão
- HALF_EVEN em tudo. Taxa: 6 casas. Fator: 4 casas. Índice: 2 casas.
- `valorConvertido`: casas ISO 4217 da moeda de destino, guardadas na nova coluna `moeda.casas_decimais` (JPY = 0, demais = 2).
- O motor multiplica `valor × taxa(6) × fator(4)` em precisão exata e arredonda **uma vez** no fim. Exemplo canônico: 100,00 × 5,421000 × 0,9850 = 533,968500 → 533,97.
- `aplicarAjusteGeopolitico = false` → fator 1.0000.
- JSON sem notação científica (`WRITE_BIGDECIMAL_AS_PLAIN`); entrada com até 6 casas e no máximo 1e12.

## Consequências
Propriedade testada: com fator ≤ 1, o resultado com ajuste nunca supera o sem ajuste (5.000 casos aleatórios em `MotorConversaoTest`).
