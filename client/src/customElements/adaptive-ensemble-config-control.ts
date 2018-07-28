import { bindable } from "aurelia-framework";
import { AdaptiveEnsembleConfig } from "../core/models/AdaptiveEnsembleConfig";
import { activationStrategy } from 'aurelia-router';

export class AdaptiveEnsembleConfigControl {

  @bindable config: AdaptiveEnsembleConfig;
  ageFunctions: string[] = ["NumberOfLabelsBased", "NumberTrainingInstancesBased"];

  bind() {
    this.setDefaultConfig();
  }

  private setDefaultConfig() {
    this.config = {
      epsilon: 0.000001,
      minTraingInstances: 2,
      retainmentFraction: 0.1,
      a: 3, //min val 2
      c: 100, //min val 2
      ageFunction: "NumberOfLabelsBased",
      individualPLTProperties: {
        hd: 32768
      }
    };
  }
}
