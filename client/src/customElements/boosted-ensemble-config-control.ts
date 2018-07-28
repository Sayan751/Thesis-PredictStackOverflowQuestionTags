import { bindable } from "aurelia-framework";
import { BoostedEnsembleConfig } from "../core/models/BoostedEnsembleConfig";
import { activationStrategy } from 'aurelia-router';

export class BoostedEnsembleConfigControl {

  @bindable config: BoostedEnsembleConfig;

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
      toAggregateByMajorityVote: true
    };
  }

  get aggrPref() {
    return this.config.toAggregateByMajorityVote ? "mv" : "fm";
  }

  set aggrPref(val: string) {
    this.config.toAggregateByMajorityVote = val === "mv";
  }

}
