package org.eclipse.milo.client

import scala.annotation.tailrec

// you can write to stdout for debugging purposes, e.g.
// println("this is a debug message")

object Solution {

  def solution(n: Int): Int = {
    // write your code in Scala 2.12
    val binary = n.toBinaryString

    // 1. Edge condition -> Check if the String contains just one 1
    if(binary.replaceAll("1", "").length() < binary.length() - 1) {
      // Remove leading & trailing zeros and recurse
      val leadingTrailingZerosRemoved = Integer.valueOf(Integer.valueOf(binary).toString.reverse).toString
      gaps(Map.empty[Int, String], leadingTrailingZerosRemoved)
    } else {
      0 // No binary gaps!
    }
  }

  @tailrec
  def gaps(acc: Map[Int, String], elems: Seq[Char]): Int = elems match {
    case Nil => acc.size
    case x :: xs if x == '1' && xs.head == '1' =>
      gaps(acc, xs.tail)
    case x :: xs if x == '1' && xs.head == '0' =>
      gaps(acc, xs)
    case x :: xs if x == '0' =>
      gaps(acc, xs)
    case x :: Nil =>
      if (acc.nonEmpty && acc.last._2.last == '0' && x == '1') {
        val (key, value) = acc.last
        val newAcc = acc.dropRight(1) ++ Map(key -> s"$value$x")
        newAcc.size
      } else acc.size
  }

  def gaps(acc: Map[Int, String], elems: String) = {

  }
}
