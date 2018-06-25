import io.gatling.core.Predef._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.feeder.FeederSupport
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._

// Simulation を継承しているクラスが全て実行される.
class LoginSimulation extends Simulation with FeederSupport {
  //
  // u000001, u000002, u000003 .... という無限リストを返す Feeder
  // ログインに使う loginId, password に利用する
  //
  val loginIdFeeder: Feeder[String] = Stream.from(1)
    .map(n => "u%06d".format(n))
    .map(id => Map("loginId" -> id, "password" -> id))
    .iterator

  //
  // i000001, i000002, i000003 .... という無限リストを返す Feeder
  // POST /api/item のパラメータに利用する
  //
  val itemFeeder: Feeder[String] = Stream.from(1)
    .map(n => "i%06d".format(n))
    .map(id => Map("name" -> id))
    .iterator

  //
  // POST /auth/login
  // loginId=u000001&password=u000001
  //
  val login: HttpRequestBuilder = http(_ => "POST /auth/login") // ここで渡した名称が統計時のラベルとなる
    .post("/auth/login") // protocol.baseURL からの相対パスを指定する
    .formParam("loginId", "${loginId}") // ${xxx} という部分は session から取り出された値に置換される.
    .formParam("password", "${password}") // loginIdFeeder により logindId, password がそれぞれ設定されているのでそちらを取り出す.
    .check(status.is(200)) // レスポンスのステータスコードが 200 であること
    .check(jsonPath("$.id").exists.saveAs("bearerToken")) // レスポンスに id 要素が存在することを確認し、session の bearerToken へその値を保存

  //
  // GET /api/items を bearerToken 付きで呼び出す
  //
  val listItems: HttpRequestBuilder = http("GET /api/item")
    .get("/api/item")
    .header("Authorization", "Bearer ${bearerToken}")
    .check(status.is(200)) // レスポンスのステータスコードが 200 であること

  //
  // POST /api/items を bearerToken 付きで呼び出す
  //
  val postItem: HttpRequestBuilder = http("POST /api/item")
    .post("/api/item")
    .header("Authorization", "Bearer ${bearerToken}")
    .header("Content-Type", "application/json")
    .body(StringBody(
      """{
        |  "name": "${name}"
        |}""".stripMargin
    ))
    .check(status.is(200)) // レスポンスのステータスコードが 200 であること

  //
  // 1 ユーザー毎に実行されるシナリオ
  //
  val scn = scenario("LoginSimulation") // シナリオ名の指定（統計時のラベルに利用される）
    .feed(loginIdFeeder) // feed に Feeder を登録することにより、session へ feeder から１行取得した値が設定される
    .exec(login) // ログインの実行
    .repeat(10, "i") { // {} 内のシナリオを 10 回繰り返す
      doIf(_ ("i").as[Int] > 0) { // 初回以外は {} 内のシナリオを実行する
        pause(3 seconds) // 3 秒待つ
      }
      .exec(listItems) // GET /api/item を呼び出す
      .pause(1 second) // 1 秒間待つ
      .feed(itemFeeder) // postItem に必要なデータを session へ登録する
      .exec(postItem) // POST /api/item を呼び出す
    }

  //
  // デフォルトの振舞いを指定する
  //
  val protocol: HttpProtocolBuilder = http
    .baseURL("http://localhost:8000") // 接続先の起点となる url
    .acceptHeader("application/json") // Accept: application/json を全てのリクエストヘッダーに自動で付与する
    .connectionHeader("keep-alive")

  setUp(
    // 実行するシナリオ、ユーザー数、シナリオの開始タイミングを指定する
    scn.inject(
      // 合計 1000 ユーザー分のシナリオを 60 秒間の間に順次投入していく
      rampUsers(1000) over (60 seconds),
    )
  )
    .throttle(
      // 最大秒間 100 リクエストに制限. 10 秒間の間に徐々に req/s を上げていく.
      reachRps(100) in (10 seconds),
      holdFor(1 hour),
    )
    .protocols(protocol)
}


