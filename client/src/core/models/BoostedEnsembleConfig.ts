import { AdaptivepltConfig } from "./AdaptivepltConfig";

export interface BoostedEnsembleConfig {
  ensembleSize: number;
  maxBranchingFactor: number;
  minEpochs: number;
  toAggregateByMajorityVote: boolean;
  individualPLTConfiguration: AdaptivepltConfig;
}

export interface BoostedEnsembleWithThresholdConfig
  extends BoostedEnsembleConfig {
  toAggregateByLambdaCW: boolean;
}
