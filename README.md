# mono-list

## 概要
Scala, Playフレームワーク, 楽天市場商品検索APIを使用して作った商品検索アプリケーションです。

## バージョン
- Play: 2.5.14
- Scala:2.11.11
- sbt: 0.13.15
- MySQL: 5.7.20

## 動かす上での前提条件
- sbtがインストールされていること。
- MySQLがインストールされていること。
- 楽天APIの利用登録がされていること。
- `/create-local-mysql-db.sh`を実行。もしくは`/create-mysql-db.sql`の内容に従ってデータベースを作成すること。
- `env/dev.conf`の下記項目を適切な値に修正すること。
    - `jdbcPassword`
    - `rakutenApplicationId`
    - `rakutenAffiliateId`(任意)

## 楽天APIの利用方法
1. 楽天の会員登録を行う。
1. 以下のリンクからアプリIDを発行する。
    1. [Rakuten Developers](https://webservice.rakuten.co.jp/)


## 実行方法
```bash
sbt flywayMigrate
sbt run
```

```bash
# sbt flywayMigrateが失敗してやり直す場合
sbt flywayClean
sbt flywayMigrate
sbt run
```
