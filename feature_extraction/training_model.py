from sklearn.svm import SVR
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import roc_auc_score, accuracy_score, average_precision_score,mean_absolute_error, mean_squared_error, r2_score
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.pipeline import Pipeline, FeatureUnion
from sklearn.svm import SVC
from sklearn.model_selection import GridSearchCV
from math import sqrt
from sklearn.decomposition import PCA
from sklearn.feature_selection import SelectKBest
import matplotlib.pyplot as plt

figNum = 1

def svr(trainX, testX, trainY):
    pca = PCA(n_components=2)
    selection = SelectKBest(k=1)
    combined_features = FeatureUnion([("pca", pca), ("univ_select", selection)])
    X_features = combined_features.fit(trainX, trainY).transform(trainX)
    clf = SVR(C=0.1, degree=100, epsilon=0.2)

    print(X_features)
    clf.fit(trainX, trainY)
    predict = clf.predict(testX)
    return predict

def gradientBoosting(trainX, testX, trainY):
    alpha = 0.95

    clf = GradientBoostingRegressor(loss='quantile', alpha=alpha,
                                    n_estimators=250, max_depth=3,
                                    learning_rate=.1, min_samples_leaf=9,
                                    min_samples_split=9)
    clf.fit(trainX, trainY)
    return clf.predict(testX)

def readData(filename):
    with open(filename, "r") as f:
        lines = f.read().split('\n')
    data = []
    for line in lines:
        splitted_line = line.split(",")
        print(len(splitted_line))
        if (len(splitted_line) > 1):
            features = [float(feature) for feature in splitted_line]
            data.append(features)
    return np.array(data)

def process(filename):
    data = readData(filename)
    trainX, testX, trainY, testY = train_test_split(data[:, :-1], data[:, -1], test_size=0.25)
    predictionSvr = svr(trainX, testX, trainY)
    predictionGradient = gradientBoosting(trainX, testX, trainY)
    mseSvr = sqrt(mean_squared_error(testY, predictionSvr))
    r2Svr = r2_score(testY, predictionSvr)
    mseGradient = sqrt(mean_squared_error(testY, predictionGradient))
    r2Gradient = r2_score(testY, predictionGradient)
    global figNum
    plt.figure(figNum)
    figNum += 1
    plt.plot(testY, predictionSvr,"o")
    plt.show()
    plt.figure(figNum)
    figNum += 1
    plt.plot(testY, predictionGradient,"o")
    plt.show()
    return mseSvr, r2Svr, mseGradient, r2Gradient

def processJobs():
    metrics = process("jobs-mod.txt")
    print("Job Metrics = ", metrics)

def processStages():
    metrics = process("stages-mod.txt")
    print("Stage Metrics = ", metrics)

if __name__ == '__main__':
    processJobs()
    processStages()