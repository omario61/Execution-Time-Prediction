from bs4 import BeautifulSoup
import urllib.request
from lxml import etree
import pydot
import numpy as np
import time
from selenium.webdriver import Firefox


mainUrl = 'http://localhost:18080'

#these data added manually by me


def readHtml(url):
    return urllib.request.urlopen(url).read()

def parseHtml(html):
    return BeautifulSoup(html, "lxml")

def toTime(time):
    if(len(time.split()) == 0):
        return "0"
    ret = float(time.split()[0])
    unit = time.split()[1].strip()
    if(unit != "ms"):
        if(unit == "s"):
            ret *= 1000
        elif (unit == "min"):
            ret *=(60*1000)
        elif(unit == "hr"):
            ret *= (60*60*1000)
        else:
            raise ValueError("Error in conversion unkown unit {u}".format(u = unit))
    return str(ret)

def toSize(size):
    if (len(size.split()) == 0):
        return "0"
    ret = float(size.split()[0])
    unit = size.split()[1].strip()
    if(unit != "KB"):
        if (unit == "B"):
            ret /= 1000
        elif (unit == "MB"):
            ret *= 1000
        elif (unit == "GB"):
            ret *= (1000 * 1000)
        else:
            raise ValueError("Error in conversion unkown unit {u}".format(u=unit))
    return str(ret)

def processGraph(stageData):
    #print(stageData)
    # rdd = ["ShuffledRDD", "MapPartitionsRDD", ""]
    nodes = stageData.find("div", {"class" :"dot-file"}).text
    graph = pydot.graph_from_dot_data(nodes)[0].get_subgraph_list();
    rddcount = 0
    for cluster in graph[0].get_subgraph_list():
        rddcount += len(cluster.get_node_list())
    return rddcount

def processTable(tableData):
    result = []
    header = [col.text for col in tableData.find("thead").findAll("th")]
    allrows = tableData.findAll("tr")
    dict = {}
    for row in allrows:
        columns = row.findAll("td")
        attrMap = {}
        for i in range(1, len(columns)):
            attrMap[header[i]] = columns[i].text
        dict[columns[0].text.replace("\n", " ").strip()] = attrMap
    return dict

def getTuple(dict, func):
    arr = []
    # print(dict["Min"])
    arr.append(func(dict["Min"]))
    arr.append(func(dict["Median"]))
    arr.append(func(dict["Max"]))
    return arr

def dummyIt():
    return ["0", "0", "0"]

def processDict(dict):
    features = []

    features.extend(getTuple(dict["Duration"], toTime) if "Duration" in dict else dummyIt())
    features.extend(getTuple(dict["Scheduler Delay"], toTime) if "Scheduler Delay" in dict else dummyIt())
    features.extend(getTuple(dict["Task Deserialization Time"], toTime) if "Task Deserialization Time" in dict else dummyIt())
    features.extend(getTuple(dict["GC Time"], toTime) if "GC Time" in dict else dummyIt())
    features.extend(getTuple(dict["Result Serialization Time"], toTime) if "Result Serialization Time" in dict else dummyIt())
    features.extend(getTuple(dict["Getting Result Time"], toTime) if "SGetting Result Time" in dict else dummyIt())
    features.extend(getTuple(dict["Peak Execution Memory"], toSize) if "Peak Execution Memory" in dict else dummyIt())
    features.extend(getTuple(dict["Output Size / Records"], toSize) if "SOutput Size / Records" in dict else dummyIt())
    features.extend(getTuple(dict["Shuffle Read Blocked Time"], toTime) if "Shuffle Read Blocked Time" in dict else dummyIt())
    features.extend(getTuple(dict["Shuffle Read Size / Records"], toSize) if "Shuffle Read Size / Records" in dict else dummyIt())
    features.extend(getTuple(dict["Shuffle Remote Reads"], toSize) if "Shuffle Remote Reads" in dict else dummyIt())

    return features

def processStage(stageData):
    stageFeatureVector = []
    rddCount = processGraph(stageData)
    dictionary = processTable(stageData.find("table", {"id" :"task-summary-table"}))
    summaryFeatures = processDict(dictionary);
    stageFeatureVector.append(str(rddCount))
    stageFeatureVector.extend(summaryFeatures)
    return stageFeatureVector

def processStageTuple(stageTuple):
    columns = stageTuple.findAll("td")
    stageLink = columns[1].find("a", {"class":"name-link"})["href"]
    print(stageLink)
    stageDuration = toTime(columns[3].text)
    print(stageDuration)
    inputSize = toSize(columns[5].text)
    outputSize = toSize(columns[6].text)
    shuffleRead = toSize(columns[7].text)
    shuffleWrite = toSize(columns[8].text)
    input =  str(float(inputSize) + float(shuffleRead))
    output = str(float(outputSize)+ float(shuffleWrite))
    features = processStage(parseHtml(readHtml(mainUrl + stageLink)))
    features.append(input)
    features.append(output)
    features.append(stageDuration)
    return features

def writeFeatureVector(filename, features):
    dataFile = open(filename, "a")
    n = len(features)
    for i in range(n-1):
        dataFile.write(str(features[i]) + ",")
    #added manually by me
    cluster_data = [ 8, 1, 8]
    for ele in cluster_data:
        dataFile.write(str(ele) + ",")
    dataFile.write(str(features[n-1]))
    dataFile.write("\n")

def aggregate(all_features):
    agg = np.zeros(len(all_features[0])-1)
    for features in all_features:
        for i in range(len(features) - 1):
            agg[i] += float(features[i])
    n = len(all_features)
    return [feature/n for feature in agg]

def processJobTuple(jobTuple):
    columns = jobTuple.findAll("td")
    jobLink = columns[1].find("a", {"class":"name-link"})["href"]
    totalJobDuration = toTime(columns[3].text)
#    print(totalJobDuration)
    stagesPage = parseHtml(readHtml(mainUrl+jobLink))
    table = stagesPage.find("tbody")
    all_features = []
    for stage in table.findAll("tr"):
        features = processStageTuple(stage)
        all_features.append(features)
        writeFeatureVector("stages-mod.txt", features)
    jobFeatures = aggregate(all_features)
    jobFeatures.append(len(all_features))
    jobFeatures.append(totalJobDuration)
    writeFeatureVector("jobs-mod.txt", jobFeatures)

def processHistory(link):
    html = parseHtml(readHtml(link))

    table_body = html.find("tbody")
    for job in table_body.findAll("tr"):
        try:
            processJobTuple(job)
        except:
            print(link)

if __name__ == '__main__':
    browser = Firefox()
    browser.get(mainUrl)

    time.sleep(30)
    parsed_html = BeautifulSoup(browser.page_source, "html.parser")
    all_history = parsed_html.find("tbody")
    for history in all_history.findAll("tr"):
        print(history.find("a")["href"])
        processHistory(mainUrl+history.find("a")["href"])