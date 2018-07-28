import { autoinject } from 'aurelia-framework';
import { I18N } from "aurelia-i18n"

const options = {
  year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', second: '2-digit',
  hour12: false
};

/**
 * Default date-time formatter
 * 
 * @export
 * @class DdfValueConverter
 */
@autoinject
export class DdfValueConverter {

  dateFormatter: any;

  constructor(private i18n: I18N) {
    this.dateFormatter = this.i18n.df(options, 'de');
  }

  toView(dateVal: any) {
    return typeof dateVal === "string"
      ? this.dateFormatter.format(new Date(dateVal))
      : this.dateFormatter.format(dateVal);
  }
}
