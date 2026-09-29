package com.alma.aitrader

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import android.app.Activity
import android.app.AlertDialog
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// ALMA AI TRADER V5.1 - beginner friendly native Android scanner.
// Market data source: Yahoo Finance chart endpoint. This is an analysis tool; it does not place XTB orders.
class MainActivity : Activity() {
    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var results: LinearLayout
    private lateinit var mode: Spinner
    private lateinit var capital: EditText
    private lateinit var risk: EditText
    private lateinit var sizeMode: Spinner
    private lateinit var fixedShares: EditText
    private lateinit var fixedValue: EditText
    private lateinit var minRR: EditText
    private lateinit var minRvol: EditText
    private lateinit var maxBreakout: EditText
    private lateinit var status: TextView

    private val tickers = listOf(
        "MU" to "USA", "AMD" to "USA", "ARM" to "USA", "QCOM" to "USA", "MSFT" to "USA", "INTC" to "USA", "NVDA" to "USA", "AMZN" to "USA",
        "SAP.DE" to "EUROPE", "ASML.AS" to "EUROPE", "AIR.PA" to "EUROPE", "GALP.LS" to "EUROPE",
        "ARM.L" to "UK", "RR.L" to "UK", "SHEL.L" to "UK"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun tv(text: String, size: Float, bold: Boolean=false): TextView = TextView(this).apply { this.text=text; textSize=size; setTextColor(Color.rgb(30,35,40)); if(bold) setTypeface(null,1); setPadding(dp(10),dp(6),dp(10),dp(6)) }
    private fun edit(value: String, hint: String): EditText = EditText(this).apply { setText(value); this.hint=hint; inputType=2; setPadding(dp(10),0,dp(10),0) }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(246,248,250)) }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(12),dp(14),dp(30)) }
        content.addView(tv("ALMA AI TRADER V5.1", 25f, true))
        content.addView(tv("Swing scanner • XTB-kompatibilis elemzés • nincs automatikus megbízás", 13f))

        content.addView(tv("MODE", 13f, true)); mode=Spinner(this); mode.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("FLEX","STANDARD","STRICT")); mode.setSelection(1); content.addView(mode)
        content.addView(tv("Tőke (RON)", 13f, true)); capital=edit("10000","10000"); content.addView(capital)
        content.addView(tv("Kockázat / trade (%)", 13f, true)); risk=edit("1.0","1.0"); content.addView(risk)
        content.addView(tv("Pozícióméret", 13f, true)); sizeMode=Spinner(this); sizeMode.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("RISK_BASED","FIXED_SHARES","FIXED_VALUE")); content.addView(sizeMode)
        content.addView(tv("Fix darabszám", 13f)); fixedShares=edit("10","10"); content.addView(fixedShares)
        content.addView(tv("Fix pozícióérték (RON)", 13f)); fixedValue=edit("2000","2000"); content.addView(fixedValue)
        content.addView(tv("Minimum R/R", 13f, true)); minRR=edit("2.0","2.0"); content.addView(minRR)
        content.addView(tv("Minimum RVOL", 13f, true)); minRvol=edit("1.5","1.5"); content.addView(minRvol)
        content.addView(tv("Maximum breakout (%)", 13f, true)); maxBreakout=edit("12","12"); content.addView(maxBreakout)

        val scan = Button(this).apply { text="🔍  SCAN"; textSize=17f; setOnClickListener { runScan() } }
        content.addView(scan, LinearLayout.LayoutParams(-1,dp(52)).apply { topMargin=dp(12); bottomMargin=dp(12) })
        status=tv("Készen áll. Nyomd meg a SCAN gombot.",13f); content.addView(status)
        content.addView(tv("EREDMÉNYEK",18f,true).apply { setPadding(10,dp(18),10,dp(8)) })
        results=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; content.addView(results)
        content.addView(tv("Megjegyzés: az adatok piaci késleltetéssel/eltéréssel jelenhetnek meg. XTB-ben az instrumentumot, spreadet és a végső árat mindig ellenőrizd.",12f).apply { setPadding(dp(10),dp(20),dp(10),0) })
        scroll.addView(content); root.addView(scroll); setContentView(root)
    }

    private fun num(e: EditText, fallback: Double)=e.text.toString().replace(',','.').toDoubleOrNull() ?: fallback
    private fun runScan() {
        results.removeAllViews(); status.text="Adatok lekérése…"
        val selectedMode=mode.selectedItem.toString(); val cap=num(capital,10000.0); val riskPct=num(risk,1.0)/100.0
        val rrMin=num(minRR,2.0); val rvolMin=num(minRvol,1.5); val boMax=num(maxBreakout,12.0)/100.0
        exec.submit {
            val out=mutableListOf<Signal>()
            for ((ticker,market) in tickers) { try { analyze(ticker,market,selectedMode,cap,riskPct,rrMin,rvolMin,boMax)?.let{out.add(it)} } catch(_:Exception){} }
            runOnUiThread { status.text="Kész: ${out.size} értékelhető jelölt"; out.sortedByDescending{it.score}.forEach{addCard(it)} }
        }
    }

    private fun yahoo(symbol:String): List<Candle> {
        val url="https://query1.finance.yahoo.com/v8/finance/chart/${symbol.replace("+","%2B")}?range=9mo&interval=1d&events=history"
        val c=URL(url).openConnection() as HttpURLConnection; c.requestMethod="GET"; c.connectTimeout=10000; c.readTimeout=15000
        val body=c.inputStream.bufferedReader().use{it.readText()}; c.disconnect()
        val r=JSONObject(body).getJSONObject("chart").getJSONArray("result").getJSONObject(0)
        val q=r.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
        val close=q.getJSONArray("close"); val high=q.getJSONArray("high"); val low=q.getJSONArray("low"); val vol=q.getJSONArray("volume")
        val list=mutableListOf<Candle>(); for(i in 0 until close.length()){ if(!close.isNull(i)&&!high.isNull(i)&&!low.isNull(i)&&!vol.isNull(i)) list.add(Candle(close.getDouble(i),high.getDouble(i),low.getDouble(i),vol.getLong(i)))}
        return list
    }
    private fun sma(a:List<Double>, n:Int)=a.takeLast(n).average()
    private fun rsi(a:List<Double>, n:Int=14):Double { var g=0.0;var l=0.0; for(i in a.size-n until a.size){val d=a[i]-a[i-1]; if(d>0)g+=d else l-=d}; if(l==0.0)return 100.0; val rs=(g/n)/(l/n); return 100-100/(1+rs) }
    private fun atr(c:List<Candle>, n:Int=14):Double { val tr=mutableListOf<Double>(); for(i in max(1,c.size-n) until c.size){tr.add(max(c[i].high-c[i].low,max(kotlin.math.abs(c[i].high-c[i-1].close),kotlin.math.abs(c[i].low-c[i-1].close))))}; return tr.average() }

    private fun analyze(ticker:String,market:String,mode:String,cap:Double,riskPct:Double,rrMin:Double,rvolMin:Double,boMax:Double):Signal? {
        val c=yahoo(ticker); if(c.size<70)return null; val closes=c.map{it.close}; val p=closes.last(); val ma20=sma(closes,20); val ma50=sma(closes,50); val resistance=c.dropLast(1).takeLast(20).maxOf{it.high}; val rvol=c.last().volume.toDouble()/c.dropLast(1).takeLast(50).map{it.volume}.average(); val r=rsi(closes); val a=atr(c); val bo=p/resistance-1
        val trend=p>ma20&&p>ma50&&ma20>ma50; val volume=rvol>=rvolMin; val momentum=r in 50.0..78.0; val breakout=bo>=0.005&&bo<=boMax; val pullback=trend&&p<=ma20*1.015&&p>=ma50*0.98&&momentum
        val setup=when{breakout&&volume->"BREAKOUT";pullback&&volume->"PULLBACK";else->"NONE"}
        val score=(if(trend)20 else 0)+(if(volume)20 else 0)+(if(momentum)15 else 0)+(if(breakout||pullback)25 else 0)+(if(p>ma20)10 else 0)+(if(p>ma50)10 else 0)
        val strictOk=when(mode){"FLEX"->score>=55;"STRICT"->score>=85;else->score>=70}
        if(!strictOk||setup=="NONE") return Signal(ticker,market,setup,"WAIT",p,p-a*1.5,p+2*(a*1.5),p+3*(a*1.5),1.0,score,0.0,rvol,r,bo)
        val sl=min(resistance*0.995,p-a*1.5); val riskPer= p-sl; if(riskPer<=0)return null; val tp1=p+2*riskPer; val tp2=p+3*riskPer; val fx=if(market=="USA") fetchFx("USDRON=X") else if(market=="UK") fetchFx("GBPRON=X") else 1.0; val riskRon=cap*riskPct; val qtyRisk=floor(riskRon/(riskPer*fx)).toInt().coerceAtLeast(0)
        val qty=when(sizeMode.selectedItem.toString()){"FIXED_SHARES"->num(fixedShares,10.0).toInt();"FIXED_VALUE"->floor(num(fixedValue,2000.0)/(p*fx)).toInt();else->qtyRisk}
        return Signal(ticker,market,setup,"BUY",p,sl,tp1,tp2,(tp1-p)/riskPer,score,qty.toDouble(),rvol,r,bo)
    }
    private fun fetchFx(symbol:String):Double { return try{yahoo(symbol).last().close}catch(_:Exception){if(symbol=="USDRON=X")4.3 else 5.0} }

    private fun addCard(s:Signal){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));setBackgroundColor(Color.WHITE)}
        val color=if(s.signal=="BUY")Color.rgb(20,130,60) else Color.rgb(190,110,20)
        box.addView(tv("${s.ticker}  •  ${s.setup}  •  ${s.signal}",19f,true).apply{setTextColor(color)})
        box.addView(tv("${s.market}   Score: ${s.score}/100   RSI: ${"%.1f".format(s.rsi)}   RVOL: ${"%.2f".format(s.rvol)}",13f))
        box.addView(tv("Entry ${"%.2f".format(s.entry)}   SL ${"%.2f".format(s.sl)}   TP1 ${"%.2f".format(s.tp1)}   TP2 ${"%.2f".format(s.tp2)}",13f))
        box.addView(tv("R/R ${"%.2f".format(s.rr)}   Qty ${s.qty.toInt()}   Breakout ${"%.1f".format(s.bo*100)}%",13f,true))
        box.addView(Button(this).apply{text="MIÉRT?";setOnClickListener{showWhy(s)}})
        results.addView(box,LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(10)})
    }
    private fun showWhy(s:Signal){AlertDialog.Builder(this).setTitle("Miért ${s.ticker}?").setMessage("Setup: ${s.setup}\n\nALMA Score: ${s.score}/100\nRSI: ${"%.1f".format(s.rsi)}\nRVOL: ${"%.2f".format(s.rvol)}\nBreakout: ${"%.2f".format(s.bo*100)}%\n\nEntry: ${"%.2f".format(s.entry)}\nSL: ${"%.2f".format(s.sl)}\nTP1: ${"%.2f".format(s.tp1)}\nTP2: ${"%.2f".format(s.tp2)}\nR/R: ${"%.2f".format(s.rr)}\nQty: ${s.qty.toInt()}\n\nA jelzés elemzési segítség, nem automatikus XTB megbízás.").setPositiveButton("OK",null).show()}

    data class Candle(val close:Double,val high:Double,val low:Double,val volume:Long)
    data class Signal(val ticker:String,val market:String,val setup:String,val signal:String,val entry:Double,val sl:Double,val tp1:Double,val tp2:Double,val rr:Double,val score:Int,val qty:Double,val rvol:Double,val rsi:Double,val bo:Double)
}
