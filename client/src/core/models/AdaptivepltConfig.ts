export interface PltConfig {
  epochs?: number;
  hd: number;
  k?: number;
}

export interface AdaptivepltConfig extends PltConfig {
  toPreferHighestProbLeaf?: boolean;
  alpha?: number;
}
