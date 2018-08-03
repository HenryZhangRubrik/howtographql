package com.howtographql.scala.sangria
import sangria.schema.{Field, ListType, ObjectType}
import models._
import sangria.schema._
import sangria.macros.derive._
import sangria.execution.deferred.Fetcher
import sangria.execution.deferred.DeferredResolver
import sangria.execution.deferred.HasId
import sangria.macros.derive._
import sangria.ast.StringValue
import akka.http.scaladsl.model.DateTime

object GraphQLSchema{
	implicit val GraphQLDateTime = ScalarType[DateTime](
		"DateTime",
		coerceOutput = (dt, _) => dt.toString,
		coerceInput = {
			case StringValue(dt, _, _) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
			case _ => Left(DateTimeCoerceViolation)

		},
		coerceUserInput = {
			case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
			case _ => Left(DateTimeCoerceViolation)
		}
	)
	val IdentifiableType = InterfaceType(
		"Identifiable",
		fields[Unit, Identifiable](
			Field("id", IntType, resolve = _.value.id)
		)
	)
	val LinkType = deriveObjectType[Unit, Link](
		Interfaces(IdentifiableType),
		ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
	)
	val UserType = deriveObjectType[Unit, User](
		Interfaces(IdentifiableType)
	)
	// implicit val userHasId = HasId[User, Int](_.id)
	// implicit val linkHasId = HasId[Link, Int](_.id)
	implicit val VoteType = deriveObjectType[Unit, Vote](
		Interfaces(IdentifiableType)
	)
	// implicit val voteHasId = HasId[Vote, Int](_.id)


 	val Id = Argument("id", IntType)
  	val Ids = Argument("ids", ListInputType(IntType))
	val linksFetcher = Fetcher(
  		(ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
	)
	val usersFetcher = Fetcher(
		(ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
	)
	val votesFetcher = Fetcher(
		(ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids)
	)
	val Resolver = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)
	val QueryType = ObjectType(
		"Query",
		fields[MyContext, Unit](
			Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
			Field("link", OptionType(LinkType), arguments = List(Argument("id", IntType)), resolve = c => linksFetcher.deferOpt(c.arg(Id))),
			Field("links", ListType(LinkType), arguments = List(Argument("ids", ListInputType(IntType))), resolve = c => linksFetcher.deferSeq(c.arg(Ids))),
			Field("users", ListType(UserType), arguments = List(Ids), resolve = c => usersFetcher.deferSeq(c.arg(Ids))),
			Field("votes", ListType(VoteType), arguments = List(Ids), resolve = c => votesFetcher.deferSeq(c.arg(Ids)))
		)


	)
	val SchemaDefinition = Schema(QueryType)
}