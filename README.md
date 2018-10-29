# squery-android
Simple Query Library for SQLite (Android/Kotlin)

開発中...

## build.gradle (App)
~~~
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"
    implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
    implementation 'com.kishe.sizuha.kotlin.squery:squery:1.0.1@aar'
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

### Table classの定義
```kotlin
import com.kishe.sizuha.kotlin.squery.Column
import com.kishe.sizuha.kotlin.squery.DateType
import com.kishe.sizuha.kotlin.squery.ISQueryRow
import com.kishe.sizuha.kotlin.squery.PrimaryKey

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
            MediaType.values().first { it.rawValue == value }
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
