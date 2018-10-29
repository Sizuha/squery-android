# squery-android
Simple Query Library for SQLite (Android/Kotlin)

開発中...
Now Developing

* 最新Version: 1.0.4
* 注意：開発中のライブラリーなので、まだ充分なテストができていません。


## build.gradle (App)
~~~
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"
    implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
    implementation 'com.kishe.sizuha.kotlin.squery:squery:1.0.4@aar'
    // . . .
}
~~~

## Open/Close a Database
```kotlin
import com.kishe.sizuha.kotlin.squery.SQuery

// Open
val db = SQuery(this, "DB_NAME", DB_VER /* 1 */ )

// Close
db.close()
```

SQLiteOpenHelperクラスみたいに、SQueryクラスをオーバーライドする事も出来る。
```kotlin
class SampleDB(context: Context, dbName: String, version: Int) : SQuery(context, dbName, version) {
    override fun onCreate(db: SQLiteDatabase?) {
        super.onCreate(db)
        // TODO ...
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // TODO ...
    }
}
```

## Create TABLE
SQueryでは、Tableの定義と行(row)データの扱いに「ISQueryRow」 interfaceを使います。

### Table classの定義
ISQueryRowを具現し、テーブル名を指定します。
```kotlin
class SampleTable : ISQueryRow {    
    override val tableName: String
        get() = "テーブル名"
}
```

次は各フィルド(column)を定義します。フィルド名、キー、Not Nullなどを「@Column」annotationで定義します。
```kotlin
@Column("フィルド名", notNull=false, unique=false, orderByAsc=true)
var fieldVar: kotlinDataType
```
@Column」はクラスのメンバー変数とプロパティ(property)に付けられます。

使えるデータの型(kotlin data types)：
* Int (INTEGER)
* Long (INTEGER)
* Boolean (INTEGER)
* Float (REAL)
* Double (REAL)
* String (TEXT)
* ByteArray (BLOB) ※ CREATE TABLEのみ
* Date (TEXT) ※ @DateType又は@TimeStampと一緒で
* Calendar (TEXT) ※ @DateType又は@TimeStampと一緒で

例)
```kotlin
class SampleTable : ISQueryRow {    
    override val tableName: String
        get() = "sample"

    // name TEXT NOT NULL
    @Column("name", notNull = true)
    var userName: String = ""
    
    // age INTEGER NOT NULL
    @Column("age", notNull = true)
    var age: Int = 0
    
    // email TEXT
    @Column("email")
    var email: String? = null    
}
```

#### Primary Key(主キー)
```kotlin
@PrimaryKey(seq=1, autoInc=false)
```
@Columnを付けたフィルドに「@PrimaryKey」を付ける。
autoInc=trueの場合、AUTOINCREMENTフィルド(INTEGER型)になる

例）
```kotlin
// idx INTEGER PRIMARY KEY AUTOINCREMENT
@Column("idx")
@PrimaryKey(autoInc=true)
private var idx = 0

/*** 複数の主キー
 * CREATE TABLE ... (first INTEGER NOT NULL, second INTEGER NOT NULL, PRIMARY KEY(first, second));
 */
@Column("first", notNull=true)
@PrimaryKey(1)
var first = 0

@Column("second", notNull=true)
@PrimaryKey(2)
var second = 0
```

#### 日付、時間フィルド
##### TEXT(DB) to Date(Kotlin)
```kotlin
@DateType("yyyy-MM-dd HH:mm:ss", timezone="")
var dateField: Date? = null
```
##### TEXT to Calendar
```kotlin
@DateType("yyyy-MM-dd HH:mm:ss", timezone="")
var dateField: Calendar? = null
```
##### TEXT to Int
```kotlin
@DateType("yyyyMMdd", timezone="")
var dateField: Int = 0 // yyyyMMdd ex) "20021231" -> 20021231
```
##### TEXT to Long(time stamp)
```kotlin
@DateType("yyyy-MM-dd HH:mm:ss", timezone="")
var dateField: Long = 0
```

#### テーブルの定義の例
```kotlin
class Anime() : ISQueryRow {
    override val tableName: String
        get() = "anime"

    @Column("idx")
    @PrimaryKey(autoInc = true)
    var idx = -1

    @Column("title", notNull = true)
    var title = ""

    @Column("title_other")
    var titleOther = ""

    @Column("start_date")
    @DateType(pattern = "yyyyMM")
    private var startDateRaw: Int = 0

    var media = MediaType.NONE

    @Column("media")
    private var mediaRaw: Int
        get() = media.rawValue
        set(value) {
            media = MediaType.values().first { it.rawValue == value }
        }

    fun getMediaText(): String = when (media) {
        MediaType.MOVIE -> "Movie"
        MediaType.OVA -> "OVA"
        MediaType.TV -> "TV"
        else -> ""
    }

    var progress: Float = 0f

    @Column("progress")
    private var progressRaw: Int
        get() = (progress*10f).toInt()
        set(value) {
            progress = value / 10f
        }

    @Column("total")
    var total = 0

    @Column("fin")
    var finished = false

    @Column("rating")
    var rating = 0 // 0 ~ 100

    val ratingAsFloat: Float
        get() = rating / 100f

    @Column("memo")
    var memo = ""

    @Column("link")
    var link = ""

    @Column("img_path")
    var imagePath = ""

    @Column("removed")
    var removed = false
}
```
