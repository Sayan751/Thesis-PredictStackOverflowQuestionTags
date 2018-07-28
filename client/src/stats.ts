import "nvd3/build/nv.d3.css"
import "jquery"
const d3 = require("d3");
import * as nv from "nvd3";
// import statData from "./data/stat.json";
const statData = require("./data/stat.json");
import rgbHex from "rgb-hex";
import hexRgb from "hex-rgb";

export class Stats {
  public width = 1200;
  public height = 600;
  private sankeyData: { nodes: { node: number, name: string }[], links: { source: number, target: number, value: number }[] };

  // activate() {
  //   let nodes: { node: number, name: string }[] = [];
  //   let links: { source: number, target: number, value: number }[] = [];
  //   const tags = new Set<string>();
  //   statData.highHighCor.forEach((d: any) => {
  //     tags.add(d.tag1);
  //     tags.add(d.tag2);
  //   });
  //   const tagMap = new Map<string, number>();
  //   let index = 0;
  //   Array.from(tags).sort().forEach((tag: string) => {
  //     tagMap.set(tag, index);
  //     nodes.push({ node: index, name: tag });
  //     index++;
  //   });
  //   console.log("tagMap:", tagMap);
  //   statData.highHighCor.forEach((d: any) => {
  //     const source: number = tagMap.get(d.tag1) || 0;
  //     const target: number = tagMap.get(d.tag2) || 0;
  //     // if (d.tag1 === "xml" || d.tag2 === "xml")
  //     //   console.warn("creating links: ", d.tag1, source, d.tag2, target)
  //     links.push({ source: source, target: target, value: d.count || 0 });
  //   });

  //   this.sankeyData = { nodes: nodes, links: links };
  //   console.log("sankeydata:", this.sankeyData);
  // }

  attached() {
    console.log("attached");
    const self = this;

    //Bar chart for high freq labels.
    nv.addGraph(() => {
      const width = self.width / 3;
      const height = self.height / 2;
      const highFreqData = statData.highFreqLabels.sort(nameComparator);
      const max = Math.max.apply(Math, statData.highFreqLabels.map(function (d:any) { return d.count; }));
      const min = Math.min.apply(Math, statData.highFreqLabels.map(function (d:any) { return d.count; }));
      const countThreshold = 9000;
      const data: any = [{ key: "Most Frequent", values: highFreqData }];

      const chart = nv.models.discreteBarChart()
        .width(width)
        .height(height)
        .x((d: any) => d.name.toLowerCase())
        .y((d: any) => d.count)
        //.staggerLabels(true)    //Too many bars and not enough room? Try staggering labels.
        .showValues(true)       //...instead, show the bar value right on top of each bar.
        .color((d: any, i: number) => {
          return `#${rgbHex(0, 200 * (d.count - min) / (max - min), 255 - (255 * (d.count - min) / (max - min)))}`
        })
        ;
      chart.options({ valueFormat: d3.format(".2s") });
      chart.xAxis.rotateLabels(-45);
      chart.yAxis.tickFormat(d3.format(".0f"));

      d3.select('#high-freq-labels')
        .attr('width', width)
        .attr('height', height)
        .datum(data)
        .transition().duration(350)
        .call(chart);

      nv.utils.windowResize(chart.update);
      return chart;
    });

    //Bar chart for low freq labels.
    nv.addGraph(() => {
      const width = self.width;// / 2;
      const height = 300;
      const lowFreqData = statData.lowFreqLabels.sort(nameComparator);

      const data: any = [{ key: "Least Frequent", values: lowFreqData }];

      const chart = nv.models.discreteBarChart()
        .width(width)
        .height(height)
        .x((d: any) => d.name.toLowerCase())
        .y((d: any) => d.count)
        .valueFormat(d3.format(".0f"))
        //.staggerLabels(true)    //Too many bars and not enough room? Try staggering labels.
        .showValues(true)       //...instead, show the bar value right on top of each bar.
        .color((d: any, i: number) => {
          return "#0000FF";
        })
        ;
      // chart.options({ valueFormat: d3.format(".1") });
      chart.xAxis.rotateLabels(-45);
      chart.yAxis.tickValues([0, 1]);

      d3.select('#low-freq-labels')
        .attr('width', width)
        .attr('height', height)
        .datum(data)
        .transition().duration(350)
        .call(chart);

      nv.utils.windowResize(chart.update);
      return chart;
    });

    //sankey
    nv.addGraph(() => {

      let nodes: { node: number, name: string }[] = [];
      let links: { source: number, target: number, value: number }[] = [];

      const max = Math.max.apply(Math, statData.highHighCor.map(function (d:any) { return d.count; }));
      const min = Math.min.apply(Math, statData.highHighCor.map(function (d:any) { return d.count; }));

      const tags = new Set<string>();
      statData.highHighCor.forEach((d: any) => {
        tags.add(d.tag1);
        tags.add(d.tag2);
      });
      const tagMap = new Map<string, number>();
      let index = 0;
      Array.from(tags).sort().forEach((tag: string) => {
        tagMap.set(tag, index);
        nodes.push({ node: index, name: tag });
        index++;
      });
      statData.highHighCor.forEach((d: any) => {
        const source: number = tagMap.get(d.tag1) || 0;
        const target: number = tagMap.get(d.tag2) || 0;
        links.push({ source: source, target: target, value: d.count || 0 });
      });

      const sankeyData = { nodes: nodes, links: links };

      const chart = nv.models.sankeyChart()
        .width(self.width)
        .height(self.height)
        .units('questions')
        // .nodeStyle({
        //   title: function (d) {
        //     return d.name + ': ' + d.value + ' ' + 'questions';
        //   },
        //   fillColor: function (d) {
        //     return d3.rgb(d.value,0,0).brighter(2);
        //   },
        //   strokeColor: function (d) {
        //     return d3.rgb(d.value,0,0).darker(2);
        //   }
        // })
        //.nodeWidth(100)
        //.nodePadding(200)
        ;

      d3.select('#high-high-sankey')
        .attr('width', self.width)
        .attr('height', self.height)
        .datum(sankeyData)
        .call(chart);

      d3.selectAll('#high-high-sankey path.link')
        .style('stroke', function (d:any) {
          console.log(d);
          // return `#${rgbHex(0, 200 * (d.value - min) / (max - min), 255 - (255 * (d.value - min) / (max - min)))}`
          return `#${rgbHex(hexRgb(d.source.color)[0], 255 * (d.value - min) / (max - min), hexRgb(d.source.color)[2])}`
        });

      nv.utils.windowResize(chart.update);
      return chart;
    });

    // //bubble plot
    // nv.addGraph(() => {
    //   const data: { key: string, values: { x: number, y: number, size: number, value: number }[] }[] = [{ key: "MostFrequent", values: [] }];
    //   const max = Math.max.apply(Math, statData.highHighCor.map(function (d) { return d.count; }));
    //   const min = Math.min.apply(Math, statData.highHighCor.map(function (d) { return d.count; }));
    //   console.log(min, max);
    //   const tags = new Set<string>();
    //   statData.highHighCor.forEach((d: any) => {
    //     tags.add(d.tag1);
    //     tags.add(d.tag2);
    //   });
    //   const tagMap = new Map<string, number>();
    //   let index = 0;
    //   const sortedTags: string[] = Array.from(tags).sort();
    //   sortedTags.forEach((tag: string) => {
    //     tagMap.set(tag, index);
    //     index++;
    //   });

    //   statData.highHighCor.forEach((d: any) => {
    //     const source: number = tagMap.get(d.tag1) || 0;
    //     const target: number = tagMap.get(d.tag2) || 0;
    //     data[0].values.push({ x: source, y: target, size: (d.count - min) / (max - min), value: d.count });
    //   });

    //   const chart = nv.models.scatterChart()
    //     .useVoronoi(true)
    //     // .color(d3.scale.category10().range())
    //     .duration(300);
    //   chart.xAxis.showMaxMin(true).tickFormat((t: number) => sortedTags[t]);
    //   chart.yAxis.showMaxMin(true).tickFormat((t: number) => sortedTags[t]);
    //   // chart.xAxis.tickFormat((t: number) => sortedTags[t]).showMaxMin(false).domain([0, 49]).range([0, self.width]);//.rotateLabels(-45)
    //   // chart.yAxis.tickFormat((t: number) => sortedTags[t]).showMaxMin(false).domain([0, 49]).range([0, 1000]);//.rotateLabels(-45)
    //   // chart.tooltip.contentGenerator((key) => {
    //   //   return key.value;
    //   // });
    //   console.log(data);

    //   d3.select('#high-high-bubble')
    //     .attr('width', self.width)
    //     .attr('height', 1000)
    //     // .transition().duration(350)
    //     .datum(data)
    //     .call(chart);
    //   return chart;
    // })
  }
}

function nameComparator(a: { name: string }, b: { name: string }) {
  if (a.name < b.name)
    return -1;
  if (a.name > b.name)
    return 1;
  return 0;
}
