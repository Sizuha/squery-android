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

## Tableの定義
SQueryでは、Tableの定義と行(row)データの扱いに「ISQueryRow」 interfaceを使う。

ISQueryRowを具現し、テーブル名を指定する。
```kotlin
class SampleTable : ISQueryRow {    
    override val tableName: String
        get() = "テーブル名"
}
```

次は各フィルド(column)を定義する。フィルド名、キー、Not Nullなどを「@Column」annotationで定義する。
```kotlin
@Column("フィルド名", notNull=false, unique=false, orderByAsc=true)
var fieldVar: kotlinDataType
```
「@Column」はクラスのメンバー変数とプロパティ(property)に付ける。

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

### Primary Key(主キー)
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
 * SQL> CREATE TABLE ... (first INTEGER NOT NULL, second INTEGER NOT NULL, PRIMARY KEY(first, second));
 */
@Column("first", notNull=true)
@PrimaryKey(1)
var first = 0

@Column("second", notNull=true)
@PrimaryKey(2)
var second = 0
```

### 日付、時間フィルド
#### TEXT(DB) to Date(Kotlin)
```kotlin
@DateType("yyyy-MM-dd HH:mm:ss", timezone="")
var dateField: Date? = null
```
#### TEXT to Calendar
```kotlin
@DateType("yyyy-MM-dd HH:mm:ss", timezone="")
var dateField: Calendar? = null
```
#### TEXT to Int
```kotlin
@DateType("yyyyMMdd", timezone="")
var dateField: Int = 0 // yyyyMMdd ex) "20021231" -> 20021231
```
#### TEXT to Long(time stamp)
```kotlin
@DateType("yyyy-MM-dd HH:mm:ss", timezone="")
var dateField: Long = 0
```
#### Timestamp(INTEGER) to Long
```kotlin
@TimeStamp(timezone="")
var dateField: Long = 0
```
#### Timestamp(INTEGER) to Date
```kotlin
@TimeStamp(timezone="")
var dateField: Date? = null
```
#### Timestamp(INTEGER) to Calendar
```kotlin
@TimeStamp(timezone="")
var dateField: Calendar? = null
```

### テーブルの定義の例
```sql
CREATE TABLE anime (
    idx INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT,
    title_other TEXT,
    start_date INTEGER,
    media INTEGER,
    progress INTEGER,
    total INTEGER,
    fin INTEGER,
    rating INTEGER,
    memo TEXT,
    link TEXT,
    img_path TEXT,
    removed INTEGER
);
```
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
    var titleOther: String? = null

    @Column("start_date", notNull=true)
    @DateType(pattern = "yyyyMM")
    var startDate: Int = 0

    var media = MediaType.NONE

    @Column("media", notNull=true)
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

    @Column("progress", notNull=true)
    private var progressRaw: Int
        get() = (progress*10f).toInt()
        set(value) {
            progress = value / 10f
        }

    @Column("total", notNull=true)
    var total = 0

    @Column("fin", notNull=true)
    var finished = false

    @Column("rating", notNull=true)
    var rating = 0 // 0 ~ 100

    val ratingAsFloat: Float
        get() = rating / 100f

    @Column("memo")
    var memo: String? = null

    @Column("link")
    var link: String? = null

    @Column("img_path")
    var imagePath: String? = null

    @Column("removed", notNull=true)
    var removed = false
}
```

## Create Table
```kotlin
val db = SQuery(this, "anime.db", DB_VER /* 1 */ )

db.from(Anime()).create(true) // true = IF NOT EXISTS
// 又は
db.createTable(Anime())
```

## Delete
```kotlin
// SQL> DROP TABLE anime;
db.from(Anime()).drop()

// SQL> DELETE FROM anime;
db.from(Anime()).delete()

// SQL> DELETE FROM anime WHERE start_date < 200001;
db.from(Anime()).where("start_date < ?", 200001).delete()
```

## Insert
```kotlin
val row = Anime().apply { 
    title = "Test Title"
    titleOther = "this is a sample"
    finished = false
    media = MediaType.MOVIE
    progress = 1f
    total = 1
    startDate = 20181001
}
db.from(row).insert()

// 又は
db.from(Anime()).values(row).insert()

// 又は
val data = ContentValues().apply { 
    put("title", "Test Title")
    put("title_other", "this is a sample")
    put("fin", false)
    // . . .
}
db.from(Anime()).values(data).insert()
```
AUTOINCREMENTのフィルドは自動で外される。

## Update
```kotlin
/*
 * SQL> UPDATE FROM anime SET title = 'Test Title', ... WHERE idx = ?;
 */
val row = Anime().apply { 
    // . . .
}
db.from(row).update()

// 又は
db.from(Anime()).values(row).update()

// 又は
val data = ContentValues().apply { 
    // . . .
}
db.from(Anime()).values(data).update()
```
where()を省略した場合、自動でWHERE句が追加される。この場合、主キー(Primary Key)を使ってWHERE句を作成する。
自動でWHERE句を作成したくない場合は、```update(false)```のように使える。

## Insert or Update
```insert()```を試して失敗したら```update()```を試す。

```kotlin
val row = Anime().apply { 
    // . . .
}
db.from(row).insertOrUpdate()

// 又は
db.from(row).where("idx = ?", row.idx).insertOrUpdate()

// 又は
db.from(Anime()).values(row).where("idx = ?", row.idx).insertOrUpdate()
```

## Select
```kotlin
// SQL> SELECT * FROM anime;
val rows = db.from(Anime()).select { Anime() } // return: MutableList<Anime>

// SQL> SELECT * FROM anime WHERE fin=1;
val rows = db.from(Anime()).where("fin=?",1).select { Anime() } // return: MutableList<Anime>

// SQL> SELECT * FROM anime WHERE fin=1 ORDER BY start_date DESC, title;
val rows = db.from(Anime())
    .where("fin=?",1)
    .orderBy("start_date", false)
    .orderBy("title")
    .select { Anime() } // return: MutableList<Anime>

// SQL> SELECT * FROM anime WHERE fin=1 ORDER BY start_date DESC, title LIMIT 0,10;
val rows = db.from(Anime())
    .where("fin=?",1)
    .orderBy("start_date", false)
    .orderBy("title")
    .limit(10,0)
    .select { Anime() } // return: MutableList<Anime>

// SQL> SELECT * FROM anime WHERE idx=100 LIMIT 1;
val rows = db.from(Anime())
    .where("idx=?",100)
    .limit(1)
    .select { Anime() } // return: MutableList<Anime>
// 又は
val row = db.from(Anime()).where("idx=?",100).selectOne { Anime() } // return: Anime
```
