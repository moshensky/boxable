package com.moshensky.boxable

import java.io.IOException

import be.quodlibet.boxable.{Cell, Table}
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem

/**
 * Created by moshensky on 7/5/15.
 */
class Row(val table: Table, var height: Float, var cells: List[Cell] = List[Cell]()) {
  private[boxable] var bookmark: PDOutlineItem = null

  def createCell(width: Float, value: String): Cell = {
    val cell: Cell = Cell(this, width, value, true)
    cells = cell :: cells
    cell
  }

  /**
   * Creates a cell with the same width as the corresponding header cell
   *
   * @param value
   * @return
   */
  def createCell(value: String): Cell = {
    val headerCellWidth: Float = table.getHeader.getCells.get(cells.size).getWidth
    val cell: Cell = Cell(this, headerCellWidth, value, false)
    cells = cell :: cells
    cell
  }

  def getHeight: Float = {
    val cellLinesCount = this.cells.foldLeft(0)((maxLinesCount, cell) => {
      val cellLinesCount = cell.getParagraph.getLines.size
      if (cellLinesCount > maxLinesCount) cellLinesCount else maxLinesCount
    })

    cellLinesCount * this.height
  }

  def getLineHeight: Float = {
    height
  }

  def setHeight(height: Float) {
    this.height = height
  }

  def getCells: List[Cell] = {
    cells
  }

  def getColCount: Int = {
    cells.size
  }

  def setCells(cells: List[Cell]) {
    this.cells = cells
  }

  def getWidth: Float = {
    table.getWidth
  }

  def getBookmark: PDOutlineItem = {
    bookmark
  }

  def setBookmark(bookmark: PDOutlineItem) {
    this.bookmark = bookmark
  }

  protected def getLastCellExtraWidth: Float = {
    var cellWidth = cells.reduce(_.getWidth + _.getWidth)
    this.getWidth - cellWidth
  }

  def xEnd: Float = {
    table.getMargin + getWidth
  }
}
