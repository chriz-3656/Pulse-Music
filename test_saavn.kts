import java.net.URL
import java.net.HttpURLConnection
import java.net.URLEncoder
import org.json.JSONObject

val query = "Queen - Bohemian Rhapsody"
val encoded = URLEncoder.encode(query, "UTF-8")
val url = URL("https://www.jiosaavn.com/api.php?__call=search.getResults&q=$encoded&p=1&n=25&_format=json&_marker=0&ctx=web6dot0")
val conn = url.openConnection() as HttpURLConnection
val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
val root = JSONObject(jsonStr)
val results = root.optJSONArray("results")

println("Results length: ${results?.length()}")
