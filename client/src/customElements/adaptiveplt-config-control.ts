import { bindable } from "aurelia-framework";
import { AdaptivepltConfig } from "../core/models/AdaptivepltConfig";

export class AdaptivepltConfigControl {
  @bindable config: AdaptivepltConfig;

  bind() {
    this.setDefaultConfig();
  }

  private setDefaultConfig() {
    this.config = {
      epochs: 1,
      hd: 32768,
      k: 2,
      alpha: 0.5,
      toPreferHighestProbLeaf: true
    };
  }
} 
