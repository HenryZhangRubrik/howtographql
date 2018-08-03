package com.howtographql.scala.sangria

import slick.jdbc.H2Profile.api._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import com.howtographql.scala.sangria.models._
import java.sql.Timestamp
import akka.http.scaladsl.model.DateTime
import scala.language.postfixOps



object DBSchema {
  implicit val dataTimeColumnType = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.clicks),
    ts => DateTime(ts.getTime)
  )
  class LinksTable(tag: Tag) extends Table[Link](tag, "LINKS"){
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def url = column[String]("URL")
    def description = column[String]("DESCRIPTION")
    def createdAt = column[DateTime]("CREATED_AT")
    def * = (id, url, description, createdAt).mapTo[Link]
  }
  class UserTable(tag: Tag) extends Table[User](tag, "USERS"){
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")
    def password = column[String]("PASSWORD")
    def createdAt = column[DateTime]("CREATED_AT")
    def * = (id, name, email, password, createdAt).mapTo[User]
  }
  class VoteTable(tag: Tag) extends Table[Vote](tag, "VOTES"){
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def createdAt = column[DateTime]("CREATED_AT")
    def userId = column[Int]("USERID")
    def linkId = column[Int]("LINKID")
    def * = (id, createdAt, userId, linkId).mapTo[Vote]
  }
  /**
    * Load schema and populate sample data withing this Sequence od DBActions
    */
  val Links = TableQuery[LinksTable]
  val Users = TableQuery[UserTable]
  val Votes = TableQuery[VoteTable]
  val databaseSetup = DBIO.seq(
    Links.schema.create,

    Links forceInsertAll Seq(
      Link(1, "http://howtographql.com", "Awesome community driven GraphQL tutorial"),
      Link(2, "http://graphql.org", "Official GraphQL web page"),
      Link(3, "https://facebook.github.io/graphql", "graphQL specification")
    ),

    Users.schema.create,
    Users forceInsertAll Seq(
      User(1, "mario", "mario@example.com", "s3cr3t", DateTime(2017, 9, 12)),
      User(2, "Fred", "fred@flinstones.com", "wilmalove", DateTime(2017, 10, 2))
    ),
    Votes.schema.create,
    Votes forceInsertAll Seq(
      Vote(id = 1, userId = 1, linkId = 1),
      Vote(id = 2, userId = 1, linkId = 2),
      Vote(id = 3, userId = 1, linkId = 3),
      Vote(id = 4, userId = 2, linkId = 2),
    )
  )




  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }

}
