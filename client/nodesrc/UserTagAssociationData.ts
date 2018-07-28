import { userIds } from './userIds';
const userTagFetchClient = require('node-fetch');
import * as fs from "fs";

const userIdPlaceHolder = "USER_ID";
const apiKey = "pj9WKZ7g4MJ6XKsKWuxKmA((";
const filter = "!--l6ygKpWzFx"; //this is for tags (without item, just the total count)
const baseUrl = `https://api.stackexchange.com/2.2/users/USER_ID/tags?site=stackoverflow&filter=${filter}&key=${apiKey}`;
const userCount = 1, minUserId = 1, maxUserId = 7838000;
let fileName = `data/userTagData.csv`;

function sleep(seconds:number) {
  var e = new Date().getTime() + (seconds * 1000);
  while (new Date().getTime() <= e) { }
}

function getRandomUserId() {
  return Math.floor(Math.random() * (maxUserId - minUserId)) + minUserId;
}

async function core(userId?: number) {
  if (!userId) userId = getRandomUserId();
  const url = `${baseUrl.replace(userIdPlaceHolder, userId.toString())}`;

  await userTagFetchClient(url, {
    method: 'GET',
  }).then((response:any) => {
    return response.json();
  }).then((data:any) => {

    if (data.backoff && data.backoff > 0) {
      sleep(data.backoff);
    }

    fs.appendFileSync(fileName, `${userId},${data.total}\n`);
    console.log(userId, data.total, data.quota_remaining);

  }).catch((err:any) => console.log(err));
}
async function run() {


  // fs.writeFileSync(fileName, "UserId,Count\n");

  // for (var i = 0; i < userCount; i++) {
  //   core();
  // }

  userIds.forEach(userId => core(userId));
}

run();
