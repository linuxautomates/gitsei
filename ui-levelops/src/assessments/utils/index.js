import { pdf } from "@react-pdf/renderer";

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export async function getFileBlob(url) {
  let response = await fetch(url);
  let data = await response.blob();
  return data;
}

export async function pdfBlob(document) {
  //await sleep(100);
  try {
    const blob = await pdf(document).toBlob();
    return blob;
  } catch (e) {
    console.log(e);
    // retry here
    await sleep(50);
    const blob = await pdf(document).toBlob();
    return blob;
  }
}
