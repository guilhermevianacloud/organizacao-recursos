# Evidência de TDD

Cada ciclo tem um commit **vermelho** (`test(red)`: testes escritos, código só com assinatura, falha **por asserção** e não por compilação) seguido de um commit **verde** (`feat(green)`: implementação mínima até a suíte inteira passar). Saídas copiadas da execução local (`./mvnw -B test` / `verify`).

## Fase 3 — núcleo de reservas

| Ciclo / cenário | Commit red | Saída no red | Commit green | Saída no green |
|---|---|---|---|---|
| 1 · RN-01 término após início; RN-02 sobreposição semiaberta (`IntervaloTest`) | `712e7a1` | `Tests run: 13, Failures: 8, Errors: 0` — ex.: "Expecting code to raise a throwable" | `84b5460` | `Tests run: 13, Failures: 0, Errors: 0` |
| 2 · RN-07 transições (49 pares); RN-08 reserva iniciada; D-08 aprovação automática (`EstadoReservaTest`, `ReservaTest`) | `50e8811` | `Tests run: 85, Failures: 55, Errors: 0` | `c65bb29` | `Tests run: 85, Failures: 0, Errors: 0` |
| 3 · RN-02 no banco (sala, professor, material); RN-04 manutenção nas fronteiras; Q-02 (`ServicoDeReservasIT`) | `c10e2c2` | integração `Tests run: 17, Failures: 8, Errors: 0` | `515d087` | integração `Tests run: 17, Failures: 0, Errors: 0` |
| 4 · RN-03 20 pedidos simultâneos (`DuplaReservaConcorrenciaIT`) | `2f6b515` | `Tests run: 7, Failures: 6, Errors: 0` — "pedidos aceitos expected: 1 but was: 10" (depois 7, 6, 4, 6; professor: 4) | `70dcfff` | 3 rodadas seguidas: unidade 85/85, integração 28/28 |

### Observações honestas sobre os ciclos
- **Testes que passaram já no vermelho**, e por quê: no ciclo 1, os 2 casos de intervalo válido (nenhuma regra recusa) e os 3 casos "não sobrepõe" (o esboço sempre respondia `false`); no ciclo 4, o controle com 20 salas diferentes (todos devem ser aceitos). São casos cujo resultado esperado coincide com o comportamento vazio — não indicam teste fraco, porque os casos opostos da mesma regra falharam.
- **Ciclo 4 teve um passo intermediário:** a primeira implementação (só a constraint) eliminou as duplicatas mas produziu deadlock intermitente — registrado como [DEF-002](../defeitos/DEF-002-deadlock-na-disputa-pelo-mesmo-recurso.md). O teste **não foi alterado**; a correção (trava por recurso) entrou no mesmo commit verde.
- **Defeito de teste encontrado e corrigido:** o teste de concorrência não limpava o banco ao terminar e quebrava o teste seguinte (`37544b6`).
- **Refatoração:** nenhum commit `refactor` nesta fase — o código verde não apresentou duplicação que justificasse.

## Evidência no CI
| Execução | Commit | Resultado |
|---|---|---|
| [41210306850](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41210306850) | `70dcfff` | verde — unidade 85, integração 28, 0 falhas |
