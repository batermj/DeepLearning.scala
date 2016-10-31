package com.thoughtworks.deepLearning
package array2D.ast

import cats.Eval
import cats.implicits._
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4s.Implicits._
import com.thoughtworks.deepLearning.array2D.utilities._
import com.thoughtworks.deepLearning.array2D.utilities.Array2DSemigroupBatch

final case class ToArray2D[Input0 <: Batch](
    operands: Vector[Vector[Ast.Aux[Input0, Batch.Aux[Eval[Double], Eval[Double]]]]])
    extends Ast {

  type Input = Input0

  final class Output private[ToArray2D] (upstreams: Vector[Vector[Batch.Aux[Eval[Double], Eval[Double]]]])
      extends Array2DSemigroupBatch {
    override def backward(delta: Eval[INDArray]): Unit = {
      for ((row, i) <- upstreams.view.zipWithIndex; (upstream, j) <- row.zipWithIndex) {
        upstream.backward(delta.map(_(i, j)))
      }

    }

    override val value = {
      upstreams.traverse(_.traverse(_.value)).map(_.toNDArray).memoize
    }

    override def close(): Unit = {
      upstreams.foreach(_.foreach(_.close()))
    }
  }

  override def forward(input: Input): Output = {
    new Output(operands.map(_.map(_.forward(input))))
  }
}
