const fs = require('fs');
const path = require('path');

const targetFile = '/home/marcos/Applications/saw-hub/design/prototipo/SAW HUB.dc.html';
let content = fs.readFileSync(targetFile, 'utf8');

// HTML for new screens
const newScreensHtml = `
    <!-- ADMIN COMERCIAL -->
    <sc-if value="{{ showAComercial }}" hint-placeholder-val="{{ true }}">
    <div style="padding:22px 32px 40px">
      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:16px">
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="display:flex;align-items:center;justify-content:space-between"><span style="font-size:12px;color:#A9A29A">Novos leads (Mês)</span><span style="font-size:11px;font-weight:600;color:#3FB27F">↑ 14%</span></div><div style="font-size:28px;font-weight:800;margin:12px 0 2px">184</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="display:flex;align-items:center;justify-content:space-between"><span style="font-size:12px;color:#A9A29A">Taxa de conversão</span><span style="font-size:11px;font-weight:600;color:#3FB27F">↑ 2.1%</span></div><div style="font-size:28px;font-weight:800;margin:12px 0 2px">18,4%</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="display:flex;align-items:center;justify-content:space-between"><span style="font-size:12px;color:#A9A29A">Novo MRR</span><span style="font-size:11px;font-weight:600;color:#3FB27F">↑ 8%</span></div><div style="font-size:28px;font-weight:800;margin:12px 0 2px">R$ 5.420</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="display:flex;align-items:center;justify-content:space-between"><span style="font-size:12px;color:#A9A29A">Vendas da Loja</span><span style="font-size:11px;font-weight:600;color:#E5573F">↓ 4%</span></div><div style="font-size:28px;font-weight:800;margin:12px 0 2px">R$ 3.850</div></div>
      </div>

      <div style="display:grid;grid-template-columns:2fr 1fr;gap:16px;margin-bottom:16px">
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:20px 22px">
          <div style="font-size:15px;font-weight:600;margin-bottom:16px">Funil de Vendas</div>
          <div style="display:flex;gap:12px;height:120px">
            <div style="flex:1;background:rgba(240,176,80,.05);border:1px solid rgba(240,176,80,.2);border-radius:12px;padding:14px;display:flex;flex-direction:column;justify-content:space-between"><div style="font-size:12px;color:#A9A29A">Leads</div><div style="font-size:22px;font-weight:700;color:#F0B050">184</div></div>
            <div style="flex:1;background:rgba(90,169,230,.05);border:1px solid rgba(90,169,230,.2);border-radius:12px;padding:14px;display:flex;flex-direction:column;justify-content:space-between"><div style="font-size:12px;color:#A9A29A">Em contato</div><div style="font-size:22px;font-weight:700;color:#5AA9E6">96</div></div>
            <div style="flex:1;background:rgba(122,35,40,.1);border:1px solid rgba(122,35,40,.3);border-radius:12px;padding:14px;display:flex;flex-direction:column;justify-content:space-between"><div style="font-size:12px;color:#A9A29A">Proposta</div><div style="font-size:22px;font-weight:700;color:#D98A8E">48</div></div>
            <div style="flex:1;background:rgba(63,178,127,.05);border:1px solid rgba(63,178,127,.2);border-radius:12px;padding:14px;display:flex;flex-direction:column;justify-content:space-between"><div style="font-size:12px;color:#A9A29A">Fechado</div><div style="font-size:22px;font-weight:700;color:#3FB27F">34</div></div>
          </div>
        </div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:20px 22px">
          <div style="font-size:15px;font-weight:600;margin-bottom:16px">Vendas por plano</div>
          <div style="display:flex;flex-direction:column;gap:14px">
            <div><div style="display:flex;justify-content:space-between;font-size:12.5px;margin-bottom:5px"><span>Profissional</span><span style="font-weight:700">18</span></div><div style="height:6px;border-radius:999px;background:#2A2724;overflow:hidden"><div style="width:53%;height:100%;background:#F0B050"></div></div></div>
            <div><div style="display:flex;justify-content:space-between;font-size:12.5px;margin-bottom:5px"><span>Essencial</span><span style="font-weight:700">10</span></div><div style="height:6px;border-radius:999px;background:#2A2724;overflow:hidden"><div style="width:29%;height:100%;background:#5AA9E6"></div></div></div>
            <div><div style="display:flex;justify-content:space-between;font-size:12.5px;margin-bottom:5px"><span>Básico</span><span style="font-weight:700">6</span></div><div style="height:6px;border-radius:999px;background:#2A2724;overflow:hidden"><div style="width:18%;height:100%;background:#3FB27F"></div></div></div>
          </div>
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:20px 22px">
          <div style="font-size:15px;font-weight:600;margin-bottom:16px">Metas do Mês</div>
          <div style="display:flex;flex-direction:column;gap:16px">
            <div><div style="display:flex;justify-content:space-between;font-size:12.5px;margin-bottom:5px"><span style="color:#A9A29A">Novos Mentorados</span><span style="font-weight:700">34 / 50 <span style="color:#6E6862;font-weight:400">(68%)</span></span></div><div style="height:6px;border-radius:999px;background:#2A2724;overflow:hidden"><div style="width:68%;height:100%;background:#F0B050"></div></div></div>
            <div><div style="display:flex;justify-content:space-between;font-size:12.5px;margin-bottom:5px"><span style="color:#A9A29A">Novo MRR</span><span style="font-weight:700">R$ 5.420 / R$ 8.000 <span style="color:#6E6862;font-weight:400">(67%)</span></span></div><div style="height:6px;border-radius:999px;background:#2A2724;overflow:hidden"><div style="width:67%;height:100%;background:#F0B050"></div></div></div>
          </div>
        </div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:20px 22px">
          <div style="font-size:15px;font-weight:600;margin-bottom:16px">Ranking do Time</div>
          <div style="display:flex;flex-direction:column;gap:12px">
            <sc-for list="{{ rankingTime }}" as="rt">
            <div style="display:flex;align-items:center;gap:12px;padding-bottom:12px;border-bottom:1px solid #1E1B19">
              <div style="font-size:13px;font-weight:700;color:#F0B050;width:14px">{{ rt.pos }}</div>
              <span style="width:32px;height:32px;border-radius:999px;background:linear-gradient(135deg,#7A2328,#57191C);display:grid;place-items:center;font:700 11px Inter,sans-serif;color:#F4EEE4">{{ rt.ini }}</span>
              <div style="flex:1"><div style="font-size:13px;font-weight:600">{{ rt.name }}</div></div>
              <div style="text-align:right"><div style="font-size:13px;font-weight:700">{{ rt.vendas }} vendas</div><div style="font-size:11px;color:#3FB27F">{{ rt.valor }}</div></div>
            </div>
            </sc-for>
          </div>
        </div>
      </div>
    </div>
    </sc-if>

    <!-- ADMIN FINANCEIRO - LANÇAMENTOS -->
    <sc-if value="{{ showAFinLancamentos }}" hint-placeholder-val="{{ true }}">
    <div style="padding:22px 32px 40px">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:16px;flex-wrap:wrap">
        <div style="display:flex;gap:12px">
          <span style="display:flex;align-items:center;height:40px;padding:0 14px;border:1px solid #2A2724;background:#141414;border-radius:10px;font-size:12.5px;color:#A9A29A">📅 Este Mês ⌄</span>
          <span style="display:flex;align-items:center;height:40px;padding:0 14px;border:1px solid #2A2724;background:#141414;border-radius:10px;font-size:12.5px;color:#A9A29A">Todas as categorias ⌄</span>
          <span style="display:flex;align-items:center;height:40px;padding:0 14px;border:1px solid #2A2724;background:#141414;border-radius:10px;font-size:12.5px;color:#A9A29A">Tipo ⌄</span>
        </div>
        <button class="saw-btn-gold" style="display:flex;align-items:center;gap:8px;height:40px;padding:0 18px;background:#F0B050;color:#1A1206;border:none;border-radius:10px;font:600 13px Inter,sans-serif;cursor:pointer"><span style="font-size:16px">+</span>Novo lançamento</button>
      </div>

      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:16px">
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">Saldo Atual</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px">R$ 42.850</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">A Receber</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px;color:#3FB27F">R$ 15.240</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">A Pagar</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px;color:#E5573F">R$ 8.450</div></div>
      </div>

      <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;overflow:hidden">
        <div style="display:grid;grid-template-columns:1fr 2fr 1.5fr 1fr 1fr 1fr .6fr;gap:14px;padding:12px 22px;font-size:10.5px;letter-spacing:.5px;color:#6E6862;text-transform:uppercase;border-bottom:1px solid #1E1B19"><div>Data</div><div>Descrição</div><div>Categoria</div><div>Conta</div><div>Valor</div><div>Status</div><div style="text-align:right">Ações</div></div>
        <sc-for list="{{ lancamentos }}" as="l" hint-placeholder-count="6">
        <div class="saw-row" style="display:grid;grid-template-columns:1fr 2fr 1.5fr 1fr 1fr 1fr .6fr;gap:14px;align-items:center;padding:13px 22px;border-bottom:1px solid #1E1B19;cursor:pointer">
          <div style="font-size:12.5px;color:#A9A29A">{{ l.date }}</div>
          <div style="font-size:13px;font-weight:600">{{ l.desc }}</div>
          <div style="font-size:12.5px;color:#A9A29A">{{ l.cat }}</div>
          <div style="font-size:12.5px;color:#A9A29A">{{ l.conta }}</div>
          <div style="font-size:13px;font-weight:700;color:{{ l.valColor }}">{{ l.valor }}</div>
          <div><span style="display:inline-flex;align-items:center;gap:6px;font-size:11px;font-weight:600;padding:3px 9px;border-radius:999px;background:{{ l.statusBg }};color:{{ l.statusColor }}">{{ l.status }}</span></div>
          <div style="text-align:right;color:#6E6862;letter-spacing:3px">✎ ⋮</div>
        </div>
        </sc-for>
      </div>
    </div>
    </sc-if>

    <!-- ADMIN FINANCEIRO - DRE -->
    <sc-if value="{{ showAFinDre }}" hint-placeholder-val="{{ true }}">
    <div style="padding:22px 32px 40px">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:16px;flex-wrap:wrap">
        <span style="display:flex;align-items:center;height:40px;padding:0 14px;border:1px solid #2A2724;background:#141414;border-radius:10px;font-size:12.5px;color:#A9A29A">📅 Ano de 2024 ⌄</span>
        <button class="saw-btn-out" style="height:40px;padding:0 16px;background:transparent;color:#F4EEE4;border:1px solid #2A2724;border-radius:10px;font:600 13px Inter,sans-serif;cursor:pointer">⤓ Exportar PDF</button>
      </div>

      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:16px">
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">Receita Bruta</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px">R$ 145.200</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">Custos Variáveis</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px;color:#E5573F">R$ 38.400</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">Despesas Fixas</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px;color:#E5573F">R$ 42.100</div></div>
        <div style="background:rgba(63,178,127,.05);border:1px solid rgba(63,178,127,.3);border-radius:16px;padding:18px"><div style="font-size:12px;color:#3FB27F">Lucro Líquido</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px;color:#3FB27F">R$ 64.700</div><div style="font-size:11px;color:#3FB27F">Margem 44,5%</div></div>
      </div>

      <div style="display:grid;grid-template-columns:1.5fr 1fr;gap:16px;margin-bottom:16px">
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;overflow:hidden">
          <div style="padding:16px 22px;border-bottom:1px solid #1E1B19;font-size:15px;font-weight:600">Demonstrativo Estruturado</div>
          <sc-for list="{{ dreLines }}" as="dl">
          <div style="display:flex;justify-content:space-between;padding:12px 22px;border-bottom:1px solid #1E1B19;background:{{ dl.bg }};font-weight:{{ dl.weight }}">
            <span style="font-size:13px;padding-left:{{ dl.pad }}">{{ dl.label }}</span>
            <span style="font-size:13px;color:{{ dl.color }}">{{ dl.val }}</span>
          </div>
          </sc-for>
        </div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:20px 22px;height:fit-content">
          <div style="font-size:15px;font-weight:600;margin-bottom:16px">Evolução do Lucro</div>
          <div style="display:flex;align-items:flex-end;gap:12px;height:180px">
            <sc-for list="{{ dreBars }}" as="b" hint-placeholder-count="6">
            <div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:8px;height:100%;justify-content:flex-end"><div style="width:100%;max-width:32px;height:{{ b.h }};background:{{ b.bg }};border-radius:4px 4px 0 0"></div><div style="font-size:11px;color:#6E6862">{{ b.m }}</div></div>
            </sc-for>
          </div>
        </div>
      </div>
    </div>
    </sc-if>

    <!-- ADMIN FINANCEIRO - FATURAMENTO -->
    <sc-if value="{{ showAFinFaturamento }}" hint-placeholder-val="{{ true }}">
    <div style="padding:22px 32px 40px">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:16px;flex-wrap:wrap">
        <span style="display:flex;align-items:center;height:40px;padding:0 14px;border:1px solid #2A2724;background:#141414;border-radius:10px;font-size:12.5px;color:#A9A29A">📅 Ano de 2024 ⌄</span>
      </div>

      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:16px">
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">Receita Recorrente (MRR)</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px">R$ 18.240</div><div style="font-size:11px;color:#3FB27F">↑ 6.2%</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">Loja SAW</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px">R$ 4.100</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">Eventos</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px">R$ 2.530</div></div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:18px"><div style="font-size:12px;color:#A9A29A">Churn Rate</div><div style="font-size:26px;font-weight:800;margin:8px 0 2px;color:#E5573F">1,8%</div><div style="font-size:11px;color:#3FB27F">Bom (Alvo < 3%)</div></div>
      </div>

      <div style="display:grid;grid-template-columns:2fr 1fr;gap:16px">
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:20px 22px">
          <div style="font-size:15px;font-weight:600;margin-bottom:16px">Faturamento por Mês e Categoria</div>
          <div style="display:flex;align-items:flex-end;gap:12px;height:240px">
            <sc-for list="{{ fatBars }}" as="b">
            <div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:8px;height:100%;justify-content:flex-end"><div style="width:100%;max-width:32px;display:flex;flex-direction:column-reverse;height:{{ b.h }}"><div style="height:{{ b.hMRR }};background:#F0B050;border-radius:0 0 0 0"></div><div style="height:{{ b.hLoja }};background:#5AA9E6;border-radius:0 0 0 0"></div><div style="height:{{ b.hEventos }};background:#3FB27F;border-radius:6px 6px 0 0"></div></div><div style="font-size:11px;color:#6E6862">{{ b.m }}</div></div>
            </sc-for>
          </div>
          <div style="display:flex;justify-content:center;gap:20px;margin-top:16px;font-size:12px;color:#A9A29A">
            <span style="display:flex;align-items:center;gap:6px"><span style="width:10px;height:10px;border-radius:3px;background:#F0B050"></span>Recorrência</span>
            <span style="display:flex;align-items:center;gap:6px"><span style="width:10px;height:10px;border-radius:3px;background:#5AA9E6"></span>Loja</span>
            <span style="display:flex;align-items:center;gap:6px"><span style="width:10px;height:10px;border-radius:3px;background:#3FB27F"></span>Eventos</span>
          </div>
        </div>
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:20px 22px">
          <div style="font-size:15px;font-weight:600;margin-bottom:16px">Composição da Receita (Anual)</div>
          <div style="display:flex;align-items:center;justify-content:center;margin:30px 0">
            <svg width="180" height="180" viewBox="0 0 42 42"><circle cx="21" cy="21" r="15.9" fill="none" stroke="#2A2724" stroke-width="8"/><circle cx="21" cy="21" r="15.9" fill="none" stroke="#F0B050" stroke-width="8" stroke-dasharray="72 28" stroke-dashoffset="25"/><circle cx="21" cy="21" r="15.9" fill="none" stroke="#5AA9E6" stroke-width="8" stroke-dasharray="18 82" stroke-dashoffset="-47"/><circle cx="21" cy="21" r="15.9" fill="none" stroke="#3FB27F" stroke-width="8" stroke-dasharray="10 90" stroke-dashoffset="-65"/></svg>
          </div>
          <div style="display:flex;flex-direction:column;gap:12px">
            <div style="display:flex;justify-content:space-between;font-size:12.5px"><span style="display:flex;align-items:center;gap:6px"><span style="width:10px;height:10px;border-radius:3px;background:#F0B050"></span>Recorrência (Planos)</span><span style="font-weight:700">72%</span></div>
            <div style="display:flex;justify-content:space-between;font-size:12.5px"><span style="display:flex;align-items:center;gap:6px"><span style="width:10px;height:10px;border-radius:3px;background:#5AA9E6"></span>Loja SAW</span><span style="font-weight:700">18%</span></div>
            <div style="display:flex;justify-content:space-between;font-size:12.5px"><span style="display:flex;align-items:center;gap:6px"><span style="width:10px;height:10px;border-radius:3px;background:#3FB27F"></span>Eventos Ingressos</span><span style="font-weight:700">10%</span></div>
          </div>
        </div>
      </div>
    </div>
    </sc-if>

    <!-- ADMIN GESTÃO DE TIME -->
    <sc-if value="{{ showATime }}" hint-placeholder-val="{{ true }}">
    <div style="padding:22px 32px 40px">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:16px;flex-wrap:wrap">
        <div style="display:flex;gap:12px">
          <div style="display:flex;align-items:center;gap:8px;height:40px;padding:0 14px;border:1px solid #2A2724;background:#141414;border-radius:10px;color:#6E6862;font-size:13px;min-width:220px">⚲ Buscar membro...</div>
          <span style="display:flex;align-items:center;height:40px;padding:0 14px;border:1px solid #2A2724;background:#141414;border-radius:10px;font-size:12.5px;color:#A9A29A">Papel ⌄</span>
        </div>
        <button class="saw-btn-gold" style="display:flex;align-items:center;gap:8px;height:40px;padding:0 18px;background:#F0B050;color:#1A1206;border:none;border-radius:10px;font:600 13px Inter,sans-serif;cursor:pointer"><span style="font-size:16px">+</span>Novo membro</button>
      </div>

      <div style="display:grid;grid-template-columns:1fr 340px;gap:16px">
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;overflow:hidden">
          <div style="display:grid;grid-template-columns:2fr 1fr 1fr 1fr .6fr;gap:14px;padding:12px 22px;font-size:10.5px;letter-spacing:.5px;color:#6E6862;text-transform:uppercase;border-bottom:1px solid #1E1B19"><div>Colaborador</div><div>Papel</div><div>Carteira (Ment.)</div><div>Conversões</div><div style="text-align:right">Ações</div></div>
          <sc-for list="{{ timeList }}" as="t">
          <div class="saw-row" style="display:grid;grid-template-columns:2fr 1fr 1fr 1fr .6fr;gap:14px;align-items:center;padding:13px 22px;border-bottom:1px solid #1E1B19;cursor:pointer">
            <div style="display:flex;gap:11px;align-items:center;min-width:0"><span style="width:36px;height:36px;flex:0 0 36px;border-radius:999px;background:linear-gradient(135deg,#7A2328,#57191C);display:grid;place-items:center;font:700 12px Inter,sans-serif;color:#F4EEE4">{{ t.ini }}</span><div style="min-width:0"><div style="font-size:13px;font-weight:600">{{ t.name }}</div><div style="font-size:11px;color:#A9A29A">{{ t.email }}</div></div></div>
            <div><span style="font-size:11px;font-weight:600;padding:3px 10px;border-radius:999px;background:{{ t.papelBg }};color:{{ t.papelColor }}">{{ t.papel }}</span></div>
            <div style="font-size:13px;font-weight:600">{{ t.carteira }}</div>
            <div style="font-size:13px;font-weight:600;color:#3FB27F">{{ t.conv }}</div>
            <div style="text-align:right;color:#6E6862;letter-spacing:3px">✎ ⋮</div>
          </div>
          </sc-for>
        </div>
        
        <div style="background:#141414;border:1px solid #2A2724;border-radius:16px;padding:24px;height:fit-content">
          <div style="text-align:center;margin-bottom:20px">
            <span style="width:80px;height:80px;border-radius:999px;background:linear-gradient(135deg,#7A2328,#57191C);display:grid;place-items:center;font:800 24px Inter,sans-serif;color:#F4EEE4;margin:0 auto 12px">MB</span>
            <div style="font-size:18px;font-weight:700">Matheus Brayan</div>
            <span style="display:inline-block;font-size:10.5px;font-weight:600;padding:3px 10px;border-radius:999px;background:rgba(240,176,80,.14);color:#F0B050;margin-top:6px">Administrador / Mentor</span>
          </div>
          
          <div style="font-size:14px;font-weight:600;margin-bottom:12px">Performance Mensal</div>
          <div style="display:flex;flex-direction:column;gap:14px;margin-bottom:24px">
            <div><div style="display:flex;justify-content:space-between;font-size:12.5px;margin-bottom:5px"><span style="color:#A9A29A">Mentorias Realizadas</span><span style="font-weight:700">42 / 50</span></div><div style="height:6px;border-radius:999px;background:#2A2724;overflow:hidden"><div style="width:84%;height:100%;background:#F0B050"></div></div></div>
            <div><div style="display:flex;justify-content:space-between;font-size:12.5px;margin-bottom:5px"><span style="color:#A9A29A">Meta de Conversão</span><span style="font-weight:700">R$ 15k / R$ 20k</span></div><div style="height:6px;border-radius:999px;background:#2A2724;overflow:hidden"><div style="width:75%;height:100%;background:#5AA9E6"></div></div></div>
          </div>
          
          <div style="font-size:14px;font-weight:600;margin-bottom:12px">Permissões de Acesso</div>
          <div style="display:flex;flex-direction:column;gap:8px">
            <div style="display:flex;align-items:center;gap:8px;font-size:13px"><span style="color:#3FB27F">✓</span> Acesso total ao Dashboard</div>
            <div style="display:flex;align-items:center;gap:8px;font-size:13px"><span style="color:#3FB27F">✓</span> Gestão de Mentorados</div>
            <div style="display:flex;align-items:center;gap:8px;font-size:13px"><span style="color:#3FB27F">✓</span> Controle Financeiro</div>
            <div style="display:flex;align-items:center;gap:8px;font-size:13px"><span style="color:#3FB27F">✓</span> Configurações da Plataforma</div>
          </div>
        </div>
      </div>
    </div>
    </sc-if>
`;

content = content.replace('<!-- ADMIN PLACEHOLDER -->', newScreensHtml + '\n    <!-- ADMIN PLACEHOLDER -->');

// JS Updates

// 1. Add finOpen to state
content = content.replace(
  "state = { screen: 'login', area: 'mentorado', ascreen: 'adashboard', rememberMe: true, showPw: false };",
  "state = { screen: 'login', area: 'mentorado', ascreen: 'adashboard', rememberMe: true, showPw: false, finOpen: false };"
);

// 2. Add asubStyle function
const asubStyleFn = `
  asubStyle(id) {
    const active = this.state.ascreen === id;
    return 'display:flex;align-items:center;gap:12px;width:100%;padding:8px 12px 8px 38px;border:none;border-radius:10px;cursor:pointer;font:500 13px Inter,sans-serif;text-align:left;transition:all .18s;'
      + 'background:' + (active ? 'rgba(240,176,80,.08)' : 'transparent') + ';'
      + 'color:' + (active ? '#F0B050' : '#A9A29A') + ';';
  }
`;
content = content.replace("navStyle(id) {", asubStyleFn + "\n  navStyle(id) {");


// 3. Define asub array and other missing variables logic 
let newLogic = `
    const asubIds = ['afinlancamentos', 'afindre', 'afinfaturamento'];
    const asub = {};
    asubIds.forEach(id => { asub[id] = { style: this.asubStyle(id), onClick: () => this.setState({ ascreen: id }) }; });
    const finActive = asubIds.includes(as);
    
    const finHeaderStyle = 'display:flex;align-items:center;gap:12px;width:100%;padding:10px 12px;border:none;border-radius:10px;cursor:pointer;font:500 14px Inter,sans-serif;text-align:left;transition:all .18s;'
      + 'background:' + (finActive ? '#221E1A' : 'transparent') + ';'
      + 'color:' + (finActive ? '#F0B050' : '#A9A29A') + ';'
      + 'box-shadow:' + (finActive ? 'inset 3px 0 0 #F0B050' : 'none') + ';';

    const rankingTime = [
      { pos: '1', ini: 'MB', name: 'Matheus Brayan', vendas: '42', valor: 'R$ 15.400' },
      { pos: '2', ini: 'LA', name: 'Lucas Alves', vendas: '28', valor: 'R$ 8.900' },
      { pos: '3', ini: 'PM', name: 'Paula Mendes', vendas: '15', valor: 'R$ 4.200' },
    ];

    const cPagar = { status: 'A pagar', statusBg: 'rgba(229,87,63,.14)', statusColor: '#E5573F', valColor: '#E5573F' };
    const cPago = { status: 'Pago', statusBg: 'rgba(63,178,127,.14)', statusColor: '#3FB27F', valColor: '#6E6862' };
    const cReceber = { status: 'A receber', statusBg: 'rgba(90,169,230,.14)', statusColor: '#5AA9E6', valColor: '#3FB27F' };
    const cRecebido = { status: 'Recebido', statusBg: 'rgba(63,178,127,.14)', statusColor: '#3FB27F', valColor: '#3FB27F' };
    
    const lancamentos = [
      { date: '28/06/2024', desc: 'Assinatura Profissional - João Silva', cat: 'Recorrência', conta: 'Mercado Pago', valor: 'R$ 297,00', ...cRecebido },
      { date: '27/06/2024', desc: 'Marketing Instagram', cat: 'Marketing', conta: 'Cartão de Crédito', valor: '- R$ 1.500,00', ...cPago },
      { date: '26/06/2024', desc: 'Kit Uniforme SAW', cat: 'Loja', conta: 'Stripe', valor: 'R$ 159,00', ...cReceber },
      { date: '25/06/2024', desc: 'Impostos (DAS)', cat: 'Tributos', conta: 'Banco Inter', valor: '- R$ 1.240,00', ...cPagar },
      { date: '25/06/2024', desc: 'Assinatura Essencial - Ana Costa', cat: 'Recorrência', conta: 'Mercado Pago', valor: 'R$ 197,00', ...cRecebido },
      { date: '24/06/2024', desc: 'Hospedagem AWS', cat: 'Infraestrutura', conta: 'Cartão de Crédito', valor: '- R$ 450,00', ...cPago },
    ];

    const dreLines = [
      { label: 'Receita Bruta', val: 'R$ 145.200', bg: '#0C0C0C', pad: '0', color: '#F4EEE4', weight: '700' },
      { label: '(-) Deduções e Impostos', val: 'R$ - 12.500', bg: '#141414', pad: '16px', color: '#E5573F', weight: '400' },
      { label: '= Receita Líquida', val: 'R$ 132.700', bg: '#0C0C0C', pad: '0', color: '#F4EEE4', weight: '700' },
      { label: '(-) Custos Operacionais', val: 'R$ - 38.400', bg: '#141414', pad: '16px', color: '#E5573F', weight: '400' },
      { label: '= Margem de Contribuição', val: 'R$ 94.300', bg: '#0C0C0C', pad: '0', color: '#F4EEE4', weight: '700' },
      { label: '(-) Despesas com Pessoal', val: 'R$ - 18.500', bg: '#141414', pad: '16px', color: '#E5573F', weight: '400' },
      { label: '(-) Despesas de Marketing', val: 'R$ - 14.200', bg: '#141414', pad: '16px', color: '#E5573F', weight: '400' },
      { label: '(-) Despesas Administrativas', val: 'R$ - 9.400', bg: '#141414', pad: '16px', color: '#E5573F', weight: '400' },
      { label: '= Lucro Líquido (EBITDA)', val: 'R$ 64.700', bg: 'rgba(63,178,127,.1)', pad: '0', color: '#3FB27F', weight: '700' },
    ];

    const dreBars = [
      { m: 'Jan', h: '40%', bg: '#3FB27F' }, { m: 'Fev', h: '35%', bg: '#3FB27F' }, { m: 'Mar', h: '50%', bg: '#3FB27F' },
      { m: 'Abr', h: '55%', bg: '#3FB27F' }, { m: 'Mai', h: '70%', bg: '#3FB27F' }, { m: 'Jun', h: '100%', bg: '#3FB27F' }
    ];

    const fatBars = [
      { m: 'Jan', h: '45%', hMRR: '70%', hLoja: '20%', hEventos: '10%' },
      { m: 'Fev', h: '50%', hMRR: '75%', hLoja: '15%', hEventos: '10%' },
      { m: 'Mar', h: '60%', hMRR: '65%', hLoja: '20%', hEventos: '15%' },
      { m: 'Abr', h: '65%', hMRR: '68%', hLoja: '18%', hEventos: '14%' },
      { m: 'Mai', h: '85%', hMRR: '70%', hLoja: '15%', hEventos: '15%' },
      { m: 'Jun', h: '100%', hMRR: '72%', hLoja: '18%', hEventos: '10%' }
    ];

    const pAdmin = { papel: 'Admin', papelBg: 'rgba(240,176,80,.14)', papelColor: '#F0B050' };
    const pMentor = { papel: 'Mentor', papelBg: 'rgba(90,169,230,.14)', papelColor: '#5AA9E6' };
    const pComercial = { papel: 'Comercial', papelBg: 'rgba(63,178,127,.14)', papelColor: '#3FB27F' };
    const pAtend = { papel: 'Atendimento', papelBg: 'rgba(110,104,98,.18)', papelColor: '#6E6862' };

    const timeList = [
      { ini: 'MB', name: 'Matheus Brayan', email: 'matheus@sawhub.com.br', carteira: '45', conv: '18,4%', ...pAdmin },
      { ini: 'LA', name: 'Lucas Alves', email: 'lucas@sawhub.com.br', carteira: '38', conv: '15,2%', ...pMentor },
      { ini: 'PM', name: 'Paula Mendes', email: 'paula@sawhub.com.br', carteira: '-', conv: '22,5%', ...pComercial },
      { ini: 'RC', name: 'Ricardo Costa', email: 'ricardo@sawhub.com.br', carteira: '42', conv: '12,8%', ...pMentor },
      { ini: 'JL', name: 'Juliana Lima', email: 'juliana@sawhub.com.br', carteira: '-', conv: '-', ...pAtend },
    ];
`;

content = content.replace("const aBuilt = ['adashboard','amentorados','amentorias','aeventos','aconteudos','arelatorios','afinanceiro','aconfig','asuporte'];", "const aBuilt = ['adashboard', 'acomercial', 'afinlancamentos', 'afindre', 'afinfaturamento', 'atime', 'amentorados','amentorias','aeventos','aconteudos','arelatorios','afinanceiro','aconfig','asuporte'];\n" + newLogic);

// 4. Update the returned object to include the new screens and data properties
const newReturnProps = `
      asub,
      finOpen: this.state.finOpen,
      finHeaderStyle,
      finChevron: this.state.finOpen ? 'rotate(180deg)' : 'rotate(0deg)',
      toggleFin: () => this.setState(st => ({ finOpen: !st.finOpen })),
      showAComercial: as === 'acomercial',
      showAFinLancamentos: as === 'afinlancamentos',
      showAFinDre: as === 'afindre',
      showAFinFaturamento: as === 'afinfaturamento',
      showATime: as === 'atime',
      rankingTime,
      lancamentos,
      dreLines, dreBars, fatBars, timeList,
`;
content = content.replace("showADashboard: as === 'adashboard',", newReturnProps + "\n      showADashboard: as === 'adashboard',");

// 5. Update aMeta definition
content = content.replace(
  "const aMeta = {",
  `const aMeta = {
      acomercial: ['Dashboard Comercial', 'Acompanhe as métricas e o funil de vendas.'],
      afinlancamentos: ['Lançamentos Financeiros', 'Controle de receitas, despesas e fluxo de caixa.'],
      afindre: ['Demonstrativo de Resultados', 'Acompanhe o DRE e a margem de lucro.'],
      afinfaturamento: ['Faturamento e MRR', 'Análise de receitas recorrentes, loja e eventos.'],
      atime: ['Gestão de Time', 'Controle da equipe SAW, carteira e performance.'],`
);

// 6. Fix aids to include new routes. The user asked for "Comercial, Financeiro (com subitens Lançamentos, DRE, Faturamento), Mentorados, Mentorias, Time, Eventos, Conteúdos, Relatórios, Configurações, Suporte."
content = content.replace(
  "const aids = ['adashboard','amentorados','amentorias','aeventos','aconteudos','arelatorios','afinanceiro','aconfig','asuporte'];",
  "const aids = ['adashboard','acomercial','afinlancamentos','afindre','afinfaturamento','amentorados','amentorias','atime','aeventos','aconteudos','arelatorios','aconfig','asuporte'];"
);

fs.writeFileSync(targetFile, content);
console.log('Patched correctly');
