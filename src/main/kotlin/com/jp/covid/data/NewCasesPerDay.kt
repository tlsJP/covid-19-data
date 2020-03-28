package com.jp.covid.data

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.stage.Stage
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.FileReader
import java.util.*
import java.util.stream.StreamSupport

/**
 * Build using: ./gradlew build
 * Run using: ./gradlew run --args="new york"
 */
class NewCasesPerDay : Application() {

    companion object {
        private var state: String? = null

        @JvmStatic
        fun main(args: Array<String>) {
            state = args.joinToString(separator = " ")
            launch(NewCasesPerDay::class.java)
        }
    }

    override fun start(stage: Stage) {

        stage.title = "Covid-19 Chart"

        // Create chart
        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        xAxis.label = "Date"
        yAxis.label = "Cases"
        val lineChart = LineChart(xAxis, yAxis)
        lineChart.title = "New Cases Per Day : $state"

        // Get Data
        val fr = FileReader("us-states.csv")
        val records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(fr)
        val spliterator = Spliterators.spliteratorUnknownSize(records.iterator(), Spliterator.ORDERED)
        val csvStream = StreamSupport.stream(spliterator, false)

        val stateRecords = csvStream
                .filter { r -> r.get("state").equals(state, ignoreCase = true) }
                .toArray()


        // Create Series
        val series = XYChart.Series<String, Number>()
        series.name = "New cases"

        var floor = 0
        for (rec in stateRecords) {
            val r = (rec as CSVRecord)
            val date = r.get("date").toString()
            val cases: Int = Integer.parseInt((r.get("cases")))
            val perDay = cases - floor
            floor = cases
            series.data.add(XYChart.Data(date, perDay))
        }

        val scene = Scene(lineChart, 800.0, 600.0)
        lineChart.data.add(series)

        stage.scene = scene
        stage.show()
    }
}