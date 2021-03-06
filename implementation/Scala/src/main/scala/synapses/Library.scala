package synapses

import synapses.model.{Draw, Mathematics}
import synapses.model.encoding.{Preprocessor, Serialization}
import synapses.model.netElems.Network.Network
import synapses.model.netElems._

import scala.util.Random

object Library {

  type ActivationFunction = Activation

  object ActivationFunction {
    val sigmoid: ActivationFunction = Activation.sigmoid
    val identity: ActivationFunction = Activation.identity
    val tanh: ActivationFunction = Activation.tanh
    val leakyReLU: ActivationFunction = Activation.leakyReLU
  }

  type NeuralNetwork = Network

  object NeuralNetwork {

    private def seedInit(maybeSeed: Option[Long],
                         layers: List[Int])
    : NeuralNetwork = {
      val layerSizes = layers.to(LazyList)
      val activationF = (_: Int) => Activation.sigmoid
      val rnd = maybeSeed match {
        case Some(i) => new Random(i)
        case None => new Random()
      }
      val weightInitF =
        (_: Int) => 1.0 - 2.0 * rnd.nextDouble()
      Network.init(layerSizes, activationF)(weightInitF)
    }

    private def throwIfInputNotMatch(network: NeuralNetwork,
                                     inputValues: List[Double])
    : Unit = {
      val numOfInputVals = inputValues.length
      val inputLayerSize = network.head.head.weights.length - 1
      if (numOfInputVals != inputLayerSize)
        throw new Exception(
          s"the number of input values ($numOfInputVals) " +
            s"does not match the size of the input layer ($inputLayerSize)"
        )
    }

    private def throwIfExpectedNotMatch(network: NeuralNetwork,
                                        expectedOutput: List[Double])
    : Unit = {
      val numOfExpectedVals = expectedOutput.length
      val outputLayerSize = network.last.length
      if (numOfExpectedVals != outputLayerSize)
        throw new Exception(
          s"the number of expected values ($numOfExpectedVals)" +
            s" does not match the size of the output layer ($outputLayerSize)"
        )
    }

    def init(layers: List[Int]): NeuralNetwork =
      seedInit(None, layers)

    def initWithSeed(seed: Long, layers: List[Int]): NeuralNetwork =
      seedInit(Some(seed), layers)

    def customizedInit(layers: List[Int],
                       activationF: Int => Activation,
                       weightInitF: Int => Double)
    : NeuralNetwork = {
      val layerSizes = layers.to(LazyList)
      Network.init(layerSizes, activationF)(weightInitF)
    }

    def prediction(network: NeuralNetwork,
                   inputValues: List[Double])
    : List[Double] = {
      throwIfInputNotMatch(network, inputValues)
      val input = inputValues.to(LazyList)
      Network
        .output(input)(network)
        .toList
    }

    def errors(network: NeuralNetwork,
               learningRate: Double,
               inputValues: List[Double],
               expectedOutput: List[Double])
    : List[Double] = {
      throwIfInputNotMatch(network, inputValues)
      throwIfExpectedNotMatch(network, expectedOutput)
      val input = inputValues.to(LazyList)
      val expected = expectedOutput.to(LazyList)
      Network
        .errors(learningRate, input, expected)(network)
        .toList
    }

    def fit(network: NeuralNetwork,
            learningRate: Double,
            inputValues: List[Double],
            expectedOutput: List[Double])
    : NeuralNetwork = {
      throwIfInputNotMatch(network, inputValues)
      throwIfExpectedNotMatch(network, expectedOutput)
      val input = inputValues.to(LazyList)
      val expected = expectedOutput.to(LazyList)
      Network.fit(learningRate, input, expected)(network)
    }

    def toJson(network: NeuralNetwork): String =
      Network.toJson(network)

    def ofJson(json: String): NeuralNetwork =
      Network
        .ofJson(json)
        .toOption
        .get

    def toSvg(network: NeuralNetwork): String =
      Draw.networkSVG(network)

  }

  type DataPreprocessor = Serialization.Preprocessor

  object DataPreprocessor {

    def init(keysWithDiscreteFlags: List[(String, Boolean)],
             datapoints: LazyList[Map[String, String]])
    : DataPreprocessor = {
      val keysWithFlags = keysWithDiscreteFlags.to(LazyList)
      Preprocessor.init(keysWithFlags, datapoints)
    }

    def encodedDatapoint(dataPreprocessor: DataPreprocessor,
                         datapoint: Map[String, String])
    : List[Double] = Preprocessor
      .encode(datapoint)(dataPreprocessor)
      .toList

    def decodedDatapoint(dataPreprocessor: DataPreprocessor,
                         encodedValues: List[Double])
    : Map[String, String] = {
      val values = encodedValues.to(LazyList)
      Preprocessor.decode(values)(dataPreprocessor)
    }

    def toJson(dataPreprocessor: DataPreprocessor): String =
      Preprocessor.toJson(dataPreprocessor)

    def ofJson(json: String): DataPreprocessor =
      Preprocessor.ofJson(json)

  }

  object Statistics {

    def rootMeanSquareError(expectedValuesWithOutputValues: LazyList[(List[Double], List[Double])])
    : Double = {
      val yHatsWithYs = expectedValuesWithOutputValues.map { case (yHat, y) =>
        (yHat.to(LazyList), y.to(LazyList))
      }
      Mathematics.rootMeanSquareError(yHatsWithYs)
    }

  }

}
