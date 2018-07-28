import { PltConfig } from "./AdaptivepltConfig";

export class AdaptiveEnsembleConfig {
  epsilon: number;
  minTraingInstances: number;
  retainmentFraction: number;
  ageFunction: string;
  a: number;
  c: number;

  individualPLTProperties: PltConfig;
}
