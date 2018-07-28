export class LnameValueConverter {
  toView(learnerClass: string) {
    switch (learnerClass) {
      case "AdaptivePLT":
        return "Adaptive PLT";
      case "PLTEnsembleBoosted":
        return "Boosted Ensemble";
      case "PLTEnsembleBoostedWithThreshold":
        return "Boosted Ensemble with Threshold";
      case "PLTAdaptiveEnsemble":
        return "Adaptive Ensemble";
      default:
        return learnerClass;
    }
  }
}
