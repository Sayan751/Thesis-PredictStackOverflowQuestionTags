import "nvd3/build/nv.d3.css"
import "jquery"
import * as d3 from "d3";
import * as nv from "nvd3";
import { DialogController } from 'aurelia-dialog';
import { inject } from "aurelia-framework";
import { CumulativeFmeasures } from "./core/models/LearnerInfo";

@inject(DialogController)
export class FmChart {

  chartData: { key: string, values: number[][] }[];
  message: string | undefined;

  constructor(private controller: DialogController) { }

  activate(cumulativeFmeasures: CumulativeFmeasures) {
    this
      .reshapeData(cumulativeFmeasures)
      .addGraph();
  }

  private reshapeData(cumulativeFmeasures: CumulativeFmeasures): FmChart {
    this.message = "Almost there; reshaping the data."
    this.chartData = [
      {
        key: "Prequential general f-measures",
        values: cumulativeFmeasures.prequentialGeneralFm
          .map((item: number, index: number) => [index + 1, item])
      },
      {
        key: "Prequential top-k f-measures",
        values: cumulativeFmeasures.prequentialTopkFm
          .map((item: number, index: number) => [index + 1, item])
      }
      // ,{
      //   key: "Post-training general f-measures",
      //   values: cumulativeFmeasures.postTrainingGeneralFm
      //     .map((item: number, index: number) => [index + 1, item])
      // },
      // {
      //   key: "Post-training top-k f-measures",
      //   values: cumulativeFmeasures.postTrainingTopkFm
      //     .map((item: number, index: number) => [index + 1, item])
      // }
    ];
    return this;
  }

  private addGraph() {
    const self = this;
    self.message = "Almost there; creating chart."
    nv.addGraph(() => {

      const chart = nv.models.lineChart()
        .x(function (d) { return d[0] })
        .y(function (d) { return d[1] })
        .duration(350)
        .color(d3.scale.category10().range())
        .margin({ left: 100 })  //Adjust chart margins to give the x-axis some breathing room.
        .useInteractiveGuideline(true)  //We want nice looking tooltips and a guideline!
        .showLegend(true)       //Show the legend, allowing users to turn on/off line series.
        .showYAxis(true)        //Show the y-axis
        .showXAxis(true)        //Show the x-axis
        ;

      chart.xAxis     //Chart x-axis settings
        .axisLabel('Instances')
        .tickFormat(d3.format(',r'));

      chart.yAxis     //Chart y-axis settings
        .axisLabel('Cumulative f-measures (square-scale)')
        .tickFormat(d3.format('.02f'));

      chart.yScale(d3.scale.pow(2).nice());
      // chart.yScale(d3.scale.log().nice());

      const width = 800;
      const height = 400;
      d3.select('#chart')
        .attr('width', width)
        .attr('height', height)
        .datum(self.chartData)
        // .transition().duration(350)
        .call(chart);

      //TODO: Figure out a good way to do this automatically
      nv.utils.windowResize(chart.update);
      self.message = undefined;
      return chart;
    });
  }
}
