# SQuery for Android(Kotlin)
Simple Query Library for SQLite (Android/Kotlin)

## build.gradle (App)
~~~
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"
    implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
    implementation 'com.kishe.sizuha.kotlin.squery:squery:1.0.13@aar'
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

// .useも使える（自動でclose）
SQuery(this, "DB_NAME", DB_VER /* 1 */ ).use {
    // . . .
}
```

SQLiteOpenHelperクラスみたいに、SQueryクラスをオーバーライドする事も出来る。
```kotlin
class SampleDB(context: Context, dbName: String, version: Int) : SQuery(context, dbName, version) {
    override fun onCreate(db: SQLiteDatabase?) {
        super.onCreate(db)
        
        // 注意！
        // onCreate()とonUpgrade()の中では「from()」メソッドは使えない！
        
        TableQuery(db!!, "TABLE_NAME").create( TABLE_CLASS(), /* ifNotExists */ true )
        
        // v1.1.18以後
        createTable(db!!, TABLE_CLASS(), /* ifNotExists */ true)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // TODO ...
    }
}
```

## Tableの定義
annotationでテーブルの定義ができる。

```kotlin
@Column("フィルド名", notNull=false, unique=false)
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
@Table("sample") // v1.1.16以上ではテーブル名をアノテーションで指定できる
class SampleTable {    
    // DB上のテーブル名: v1.0.15以下の場合
    companion object {
        val tableName = "sample"
    }
        
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
autoInc=trueの場合、AUTOINCREMENTフィルド(INTEGER型)になる。

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
@Column("date")
@DateType("yyyy-MM-dd HH:mm:ss", timezone="") // timezoneは省略可
var dateField: Date? = null
```
#### TEXT to Calendar
```kotlin
@Column("date")
@DateType("yyyy-MM-dd HH:mm:ss", timezone="")
var dateField: Calendar? = null
```
#### TEXT to Int
```kotlin
@Column("date")
@DateType("yyyyMMdd", timezone="")
var dateField: Int = 0 // yyyyMMdd ex) "20021231" -> 20021231
```
#### TEXT to Long(time stamp)
```kotlin
@Column("date")
@DateType("yyyy-MM-dd HH:mm:ss", timezone="")
var dateField: Long = 0
```
#### Timestamp(INTEGER) to Long
```kotlin
@Column("date")
@TimeStamp(timezone="")
var dateField: Long = 0
```
#### Timestamp(INTEGER) to Date
```kotlin
@Column("date")
@TimeStamp(timezone="")
var dateField: Date? = null
```
#### Timestamp(INTEGER) to Calendar
```kotlin
@Column("date")
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
@Table("anime") // v1.1.16以上の場合
class Anime() {
    companion object { val tableName = "anime" } // v1.0.15以下の場合

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

### 制限、仮定
SQueryは単純なケースのQueryをなるべく自動かするのが目標なので、色々制限がある。もし、もっと詳細なQueryが必要な場合は`SQuery`クラスの`rawQuery(sql: String, args: Array<out String>): Cursor?`又は`execute(sql: String, args: Array<out String>)`メソッドを使う。

#### CREATE TABLEで、AUTO INCREMENTの扱い
* CREATE TABLEの時、`@PrimaryKey(autoInc=true)`のフィルドがある場合、これがテーブルの中で唯一の主キーであること。

## Create Table
```kotlin
val db = SQuery(this, "anime.db", DB_VER /* 1 */ )

// v1.0.15以下
db.from(Anime.tableName).create(Anime(), true) // true = IF NOT EXISTS

// v1.1.16から
db..createTable(Anime(), true)
```

## Delete
```kotlin
// SQL> DROP TABLE anime;
db.from(Anime.tableName).drop()
db.from<Anime>().drop() // v1.1.16から

// SQL> DELETE FROM anime;
db.from(Anime.tableName).delete()
db.from<Anime>().delete() // v1.1.16から

// SQL> DELETE FROM anime WHERE start_date < 200001;
db.from(Anime.tableName).where("start_date < ?", 200001).delete()
db.from<Anime>().where("start_date < ?", 200001).delete() // v1.1.16から
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
// AUTOINCREMENTのフィルドは自動で外される。
db.from<Anime>().insert(row)

// 又は
val data = ContentValues().apply { 
    put("title", "Test Title")
    put("title_other", "this is a sample")
    put("fin", false)
    // . . .
}
db.from<Anime>().insert(data)
```

## Update
```kotlin
/*
 * SQL> UPDATE FROM anime SET title = 'Test Title', ... WHERE idx = ?;
 */
val row = Anime().apply { 
    // . . .
}
db.from<Anime>().update(row)

// 又は
val data = ContentValues().apply { 
    // . . .
}
db.from<Anime>().update(data)
```
where()を省略した場合、自動でWHERE句が追加される。この場合、主キー(Primary Key)を使ってWHERE句を作成する。
自動でWHERE句を作成したくない場合は、`update(false)`のように使える。

主キー(`@PrimaryKey`)で指定されたフィルドは`SET`の内容から自動で外される。

## Insert or Update
`insert()`を試して失敗したら`update()`を試す。

```kotlin
val row = Anime().apply { 
    // . . .
}
db.from<Anime>().where("idx=?", row.idx).insertOrUpdate(row)

// 又は (WHERE句を自動で作成する)
db.from<Anime>().insertOrUpdate(row)
```

## Update or Insert
`update()`を試して失敗したら`insert()`を試す。
```kotlin
val row = Anime().apply { 
    // . . .
}
db.from<Anime>().where("idx=?", row.idx).updateOrInsert(row)

// 又は (WHERE句を自動で作成する)
db.from<Anime>().insertOrUpdate(row)
```

## Select
```kotlin
// SQL> SELECT * FROM anime;
val rows = db.from<Anime>().select { Anime() } // return: MutableList<Anime>

// SQL> SELECT * FROM anime WHERE fin=1;
val rows = db.from<Anime>().where("fin=?",1).select { Anime()} // return: MutableList<Anime>

// SQL> SELECT * FROM anime WHERE fin=1 ORDER BY start_date DESC, title LIMIT 0,10;
val rows = db.from<Anime>()
    .where("fin=?",1)
    .orderBy("start_date", false)
    .orderBy("title")
    .limit(10,0)
    .select { Anime() } // return: MutableList<Anime>

// SQL> SELECT * FROM anime WHERE idx=100 LIMIT 1;
val rows = db.from<Anime>()
    .where("idx=?",100)
    .limit(1)
    .select { Anime() } // return: MutableList<Anime>
// 又は
val row = db.from<Anime>().where("idx=?",100).selectOne { Anime() } // return: Anime or null

// SQL> SELECT title FROM anime WHERE start_date < 200001;
class AnimeTitle() {
    @Column("title", notNull=true)
    var title: String
}
val rows = db.from("anime")
    .columns("title")
    .where("start_date < ?", 200001)
    .select { AnimeTitle() }
```
他にも、`groupBy()`、`having()`、`distinct()`なども使える。

JOIN機能はまだテスト中。

### 結果をCursorで返す
```kotlin
// SQL> SELECT * FROM anime WHERE fin=0;
val cursor = db.from<Anime>().where("fin=?",0).selectAsCursor() // return: Cursor
cursor.use {
// . . .
}

// SQL> SELECT title, progress, total FROM anime WHERE fin=0;
val cursor = db.from<Anime>().columns("title","progress","total").where("fin=?",0).selectAsCursor()
// 又は
val cursor = db.from<Anime>().where("fin=?",0).selectAsCursor("title","progress","total")
```

### Cursorからオブジェクトを作成する
```kotlin
val cursor = db.from<Anime>().where("fin=?",0).selectAsCursor()
cursor.use {
    cursor.moveFirst()
    do {
        val obj = convertFromCursor(cursor) { Anime() }
        // . . .
    } while(cursor.moveNext())    
}
```

### 手動でCursorからオブジェクトに変換
```kotlin
// SQL> SELECT title, progress, total FROM anime WHERE fin=0;
val rows = db.from<Anime>()
    .columns("title","progress","total")
    .where("fin=?",0)
    .selectWithCursor { cursor -> 
        Anime().let { row ->
            cursor.run {
                var i: Int

                i = getColumnIndex("title")
                row.title = getString(i)

                i = getColumnIndex("progress")
                row.progress = getInt(i) / 10f

                i = getColumnIndex("total")
                row.total = getInt(i)
            }
            row // return (※ 返すオブジェクトはISqueryRow型でなくても大丈夫)
        }
    } // return: MutableList<Anime>
```

### ForEach
```kotlin
// SQL> SELECT * FROM anime;
db.from<Anime>().selectForEach({ Anime() }) { row -> // row: Anime
    row.run {
    // . . .
    }
}

// 又は
db.from<Anime>().selectForEachCursor { cursor ->
    cursor.run {
    // . . .
    }
}
```

## Where
WHERE句で条件をANDでつなげる事がよくある。この場合`whereAnd()`メソッドが使える。
```kotlin
// SQL> SELECT * FROM anime WHERE (fin=0) AND (media=1) AND (start_date>200000);
val rows = db.from<Anime>()
    .where("fin=?", 0)
    .whereAnd("media=?", 1)
    .whereAnd("start_date>?", 200000)
    .select { Anime() }
// 又は    
val rows = db.from<Anime>()
    .whereAnd("fin=?", 0)
    .whereAnd("media=?", 1)
    .whereAnd("start_date>?", 200000)
    .select { Anime() }
```

## 「読み込み・書き込み」をカスタマイズ
```kotlin
@Table("anime") // v1.1.16から
class Anime() : ISQueryRow {
    
    // manually=trueのフィルドは、DBから自動でデータの読み込みや書き込みを行わない
    @Column("rating", manually=true) 
    var rating = 0f // ex) 1f = 100, 0.5f = 50
    
    // ...省略...
    
    override fun toValues(): ContentValues? {
        return ContentValues().apply {
            put("rating", (rating*100).toInt())
        }
    }
    
    override fun loadFrom(cursor: Cursor) {
        cursor.run {
            var i = getColumnIndex("rating")
            rating = getInt(i) / 100f
        }
    }
}
```
