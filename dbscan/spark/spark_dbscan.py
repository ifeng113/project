# -*- coding:utf-8 -*-
import os
import sys
import time
import uuid
import io
from _codecs import decode
from imp import reload

from pyspark import SparkContext, SparkConf
from math import asin, sqrt, sin, cos, radians

if __name__ == "__main__":

    # 设置编码
    # reload(sys)
    # sys.setdefaultencoding("utf8")

    def getDistance(lon1, lat1, lon2, lat2):  # 经度1，纬度1，经度2，纬度2 （十进制度数）
        if lon1 == lon2 and lat1 == lat2:
            return 0.0
        # 将十进制度数转化为弧度
        lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])

        dlon = lon2 - lon1
        dlat = lat2 - lat1
        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * asin(sqrt(a))
        r = 6371  # 地球平均半径，单位为千米
        return c * r * 1000

    def locationDistance(lon1, lat1, lon2, lat2, epsDistance):
        if abs(lon1 - lon2) > epsDistance or abs(lat1 - lat2) > epsDistance:
            return epsDistance
        return getDistance(int(lon1) / 1000000.0, int(lat1) / 1000000.0, int(lon2) / 1000000.0, int(lat2) / 1000000.0)

    conf = SparkConf()
    conf.set("spark.default.parallelism", 12)
    sc = SparkContext(appName="sparkDBSCAN", conf=conf)

    def transform(point):
        location = tuple(point.split(","))
        # print(int(location[0]), int(location[1]))
        return int(location[0]), int(location[1])
    lines_rdd = sc.textFile(sys.argv[1], 4).map(lambda x: transform(x)).cache()

    Eps = int(sys.argv[3])
    EpsDistance = (Eps * 1000000.0) / 111000.0
    MinPts = int(sys.argv[4])
    code = sys.argv[2]
    callback = sys.argv[5]
    result_path = sys.argv[6]

    data = lines_rdd.collect()

    def transformCorePoint(pData, x, eps, minPts, epsDistance):
        nArray = []
        for index, p in enumerate(pData):
            if locationDistance(p[0], p[1], x[0], x[1], epsDistance) <= eps:
                nArray.append(p)
                if len(nArray) == minPts:
                    break
        return x[0], x[1], nArray

    point_rdd = lines_rdd.map(lambda x: transformCorePoint(data, x, Eps, MinPts, EpsDistance)).cache()

    core_point_rdd = point_rdd.filter(lambda x: len(x[2]) == MinPts).cache()

    def mergePoint(x):
        return [[(x[0], x[1])]]
    core_point_merge = core_point_rdd.map(lambda x: mergePoint(x)).cache()

    # for i in core_point_merge.collect():
    #     #         print(i)

    startReduce = int(time.time())
    print("------------------ reduce start", startReduce)

    def mergeCorePointCluster(x, y, eps, epsDistance):

        reduceOne = int(time.time())

        # todo 可优化
        # 遍历A组的簇
        for index, c in enumerate(x):
            # 遍历A组某一簇的点
            contain = False
            containCluster = []
            for spc in c:
                # 遍历B组的簇
                for m in y:
                    # 遍历B组某一簇的点
                    for mpc in m:
                        # print("------", spc, mpc)
                        if locationDistance(spc[0], spc[1], mpc[0], mpc[1], epsDistance) <= eps:
                            # 找出合并簇
                            contain = True
                            if m not in containCluster:
                                containCluster.append(m)
                            break
            if contain:
                for j in containCluster:
                    # 扩展A组的某一簇的点(与B组的某些簇具有关系链)
                    for k in j:
                        x[index].append(k)
                    # 删除B组中已归簇的
                    y.remove(j)
        if y:
            # 扩展A组的簇
            for k in y:
                x.append(k)
        reduceOneEnd = int(time.time())
        if reduceOneEnd - reduceOne > 10:
            print("------------------ once reduce", reduceOneEnd - reduceOne)
        return x

    if core_point_merge.count():
        core_point_merge = core_point_merge.reduce(lambda x, y: mergeCorePointCluster(x, y, Eps, EpsDistance))

    print("------------------ reduce used time: ", int(time.time()) - startReduce)

    # 组装数据
    curl_body = "{\"code\":\"" + code + "\",\"data\":["
    if len(core_point_merge):
        for d1 in core_point_merge:
            curl_body += "".join(d1.__str__()) + ","
        curl_body = curl_body.replace("(", "\"")
        curl_body = curl_body.replace(")", "\"")
        curl_body = curl_body.replace(" ", "")
        curl_body = curl_body.strip(',')
    curl_body += "]}"

    file_name = uuid.uuid1().__str__()
    file_suffix = ".txt"
    file_path = result_path + file_name + file_suffix
    file = io.open(file_path, "wb")
    file.write(curl_body)
    file.close()

    curl = "curl " + callback + " -X POST -H 'Content-Type:application/json' -d " + file_name + file_suffix

    print(curl)

    os.system(curl)
    # print("通过请求返回结果数据")
    sc.stop()
