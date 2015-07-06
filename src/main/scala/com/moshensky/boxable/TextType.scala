package com.moshensky.boxable

/**
 * Created by moshensky on 7/6/15.
 */
sealed trait TextType
object TextType {
  case object Highlight extends TextType
  case object Underline extends TextType
  case object Squiggly extends TextType
  case object Strikeout extends TextType
}
