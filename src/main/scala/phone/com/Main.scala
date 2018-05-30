package phone.com

import java.nio.file.{InvalidPathException, Path, Paths}

import cats.effect.{Effect, IO, Sync}
import cats.syntax.either._
import fs2.{Stream, StreamApp, io, text}

object Main extends StreamApp[IO] {
  case class InvalidResourceException(msg: String) extends RuntimeException(msg)

  private val CHUNK_SIZE = 4096
  def fromEither[F[_]: Effect, A](thunk: => Either[Throwable, A]): Stream[F, A] =
    Stream.eval(Effect[F].fromEither(thunk))

  def getPath(path: String): Either[Throwable, Path] =
    Either.catchOnly[InvalidPathException](Paths.get(path))

  def printStream[F[_]](reports: Stream[F, String])(implicit F: Sync[F]): Stream[F, String] =
    reports.flatMap(r => Stream.eval_(F.delay(println(r))))

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] =
    for {
      path <- fromEither[IO, Path](getPath(args.headOption.getOrElse("src/main/resources/calls.log")))
      _ <- io.file.readAll[IO](path, CHUNK_SIZE)
        .through(text.utf8Decode)
        .through(text.lines)
        .through(TotalCostReport.readCallsFromLog[IO])
        .through(TotalCostReport.report[IO])
        .through(printStream[IO])
    } yield StreamApp.ExitCode(0)

}
