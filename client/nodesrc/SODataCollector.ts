const fetchClient = require('node-fetch');
import * as moment from "moment";
import * as fs from "fs";

const apiTypes = { questions: "questions", users: "users", tags:"tags" };
const apiTypePlaceHolder = "API_TYPE";
const apiKey = "pj9WKZ7g4MJ6XKsKWuxKmA((";
// const filter = "!9YdnSPuG8"
const filter = "!--l6ygKpWzFx"; //this is for tags (without item, just the total count)
const baseUrl = `https://api.stackexchange.com/2.2/${apiTypePlaceHolder}?site=stackoverflow&filter=${filter}&key=${apiKey}`; //order=desc&sort=activity&
const dateformat = "YYYY-M-D";

function sleep(seconds:number) {
  var e = new Date().getTime() + (seconds * 1000);
  while (new Date().getTime() <= e) { }
}

async function run(apiType: string) {

  let from = moment.utc("2008-08-01").startOf("month");
  let to = moment.utc("2008-08-31").endOf("month");
  // let endDate = moment.utc("2016-12-31").hour(23).minute(59).second(59).valueOf();
  let endDate = moment.utc("2017-03-31").hour(23).minute(59).second(59).valueOf();
  let fileName = `data/${apiType}Data.csv`;


  fs.writeFileSync(fileName, "Month, Count\n");

  while (endDate > from.valueOf()) {
    const url = `${baseUrl.replace(apiTypePlaceHolder, apiType)}&fromdate=${from.unix()}&todate=${to.unix()}`;

    await fetchClient(url, {
      method: 'GET',
    }).then((response:any) => {
      return response.json();
    }).then((data:any) => {

      if (data.backoff && data.backoff > 0) {
        sleep(data.backoff);
      }

      fs.appendFileSync(fileName, `${from.format(dateformat)},${data.total}\n`);
      console.log(apiType, from.format(dateformat), data.total, data.quota_remaining);

    }).catch((err:any) => console.log(err));

    from.add(1, "month").startOf("month");
    to.add(1, "month").endOf("month");
  }
}

// run(apiTypes.questions);
// run(apiTypes.users);
run(apiTypes.tags);
