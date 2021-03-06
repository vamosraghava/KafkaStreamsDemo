package stateless.transformations

import java.io._
import java.util.Properties

import org.apache.kafka.common.serialization._
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.{ConsumerRecordFactory, OutputVerifier}
import org.scalatest._
import utils.Settings

import scala.io.Source

class PeekTopologyTests
  extends FunSuite
  with BeforeAndAfter
  with Matchers {

  val props = Settings.createBasicStreamProperties("stateless-peek-application", "localhost:9092")
  val integerDeserializer: IntegerDeserializer = new IntegerDeserializer

  before {
  }

  after {
  }


  test("Should produce correct output") {

    //arrange

    val path ="c:\\temp\\kafka-streams-peek.txt"
    new File(path).delete()
    val pw = new PrintWriter(new File(path))

    val recordFactory: ConsumerRecordFactory[java.lang.Integer, java.lang.Integer] =
      new ConsumerRecordFactory[java.lang.Integer, java.lang.Integer](new IntegerSerializer, new IntegerSerializer)
    val peekTopology = new PeekTopology(pw)
    val testDriver = new TopologyTestDriver(peekTopology.createTopolgy(), props)

    //act
    testDriver.pipeInput(recordFactory.create("InputTopic", 1, 1, 9995L))
    testDriver.pipeInput(recordFactory.create("InputTopic", 1, 2, 9995L + 1))

    //assert
    val lines = Source.fromFile(path).getLines.toList

    assert(lines.length == 2)
    assert(lines(0) == "Saw input value line '1'")
    assert(lines(1) == "Saw input value line '2'")

    OutputVerifier.compareValue(testDriver.readOutput("peekedOutputTopic", integerDeserializer, integerDeserializer),
      1.asInstanceOf[Integer])
    OutputVerifier.compareValue(testDriver.readOutput("peekedOutputTopic", integerDeserializer, integerDeserializer),
      2.asInstanceOf[Integer])

    val result1 = testDriver.readOutput("peekedOutputTopic", integerDeserializer, integerDeserializer)
    assert(result1 == null)

    peekTopology.stop()
    cleanup(props, testDriver)
  }


  def cleanup(props:Properties, testDriver: TopologyTestDriver) = {

    try {
      //there is a bug on windows which causes this line to throw exception
      testDriver.close
    } catch {
      case e: Exception => {
        delete(new File("C:\\data\\kafka-streams"))
      }
    }
  }

  def delete(file: File) {
    if (file.isDirectory)
      Option(file.listFiles).map(_.toList).getOrElse(Nil).foreach(delete(_))
    file.delete
  }
}