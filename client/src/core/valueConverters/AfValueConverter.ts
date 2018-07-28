export class AfValueConverter {
  toView(af: string) {
    switch (af) {
      case "NumberOfLabelsBased":
        return "Number of labels based";
      case "NumberTrainingInstancesBased":
        return "Number of training instances based";
      default:
        return af;
    }
  }
}
