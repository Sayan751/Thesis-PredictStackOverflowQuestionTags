import { bindable } from "aurelia-framework";
import { BoostedEnsembleWithThresholdConfig } from "../core/models/BoostedEnsembleConfig";
import { activationStrategy } from 'aurelia-router';

export class BoostedEnsembleWiththresholdConfigControl {

  @bindable config: BoostedEnsembleWithThresholdConfig;

  bind() {
    this.setDefaultConfig();
  }

  private setDefaultConfig() {
    this.config = {
      ensembleSize: 10,
      maxBranchingFactor: 10,
      minEpochs: 1,
      individualPLTConfiguration: {
        hd: 32768
      },
      toAggregateByLambdaCW: true,
      toAggregateByMajorityVote: false
    };

    // this.config.ensembleSize = 10;
    // this.config.maxBranchingFactor = 10;
    // this.config.minEpochs = 1;
    // this.config.individualPLTConfiguration = {
    //   hd: 32768
    // }
    // this.config.toAggregateByLambdaCW = true;
  }

  get aggrPref() {
    if (this.config.toAggregateByLambdaCW) return "lcw";
    if (this.config.toAggregateByMajorityVote) return "mv";
    if (!this.config.toAggregateByLambdaCW && !this.config.toAggregateByMajorityVote) return "fm";
    else throw Error("Invalid aggregation preference");
  }

  set aggrPref(val: string) {
    this.config.toAggregateByLambdaCW = val === "lcw";
    this.config.toAggregateByMajorityVote = val === "mv";
  }
}
