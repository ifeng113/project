# -*- coding:utf-8 -*-
# import pymssql
# import pymysql
import sys
from imp import reload
from pyspark import SparkContext
from math import asin, sqrt, sin, cos, radians

if __name__ == "__main__":

    # 设置编码
    reload(sys)
    sys.setdefaultencoding("utf8")

    def getLocation(point):
        location = tuple(point.split(", "))
        return int(location[14]), int(location[15][:-1]), 0

    def distance(lon1, lat1, lon2, lat2):  # 经度1，纬度1，经度2，纬度2 （十进制度数）

        # 将十进制度数转化为弧度
        lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])

        dlon = lon2 - lon1
        dlat = lat2 - lat1
        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * asin(sqrt(a))
        r = 6371  # 地球平均半径，单位为千米
        return c * r * 1000

    def alarmCalc(circle_distance, alarm_threshold, base_alarm):   # 集合中心最小距离，报警阈值，报警基础集合
        # 报警集合
        circle = []
        # 报警过滤集合
        alarm = []
        for i in base_alarm:
            no = True
            for j in range(len(circle)):
                if distance(i[0] / 1000000, i[1] / 1000000, circle[j][0] / 1000000, circle[j][1] / 1000000) \
                        < circle_distance:
                    no = False
                    circle[j] = (circle[j][0], circle[j][1], circle[j][2] + 1)
                    break
            if no:
                param = (i[0], i[1], i[2])
                circle.append(param)
        for i in circle:
            if i[2] >= alarm_threshold:
                alarm.append(i)
        return alarm

    def alarmSort(alarm):
        # 排序报警集合
        sort_alarm = []
        for j in range(len(alarm)):
            max_number = j
            for k in range(len(alarm)):
                if k > j and alarm[max_number][2] < alarm[k][2]:
                    max_number = k
            sort_alarm.append(alarm[max_number])
            # 已放入排序的数据置为0
            alarm[max_number] = (alarm[max_number][0], alarm[max_number][1], 0)
        return sort_alarm

    sc = SparkContext(appName="locationCalc")
    lines = sc.textFile(sys.argv[1], 1)
    data = lines.map(lambda x: getLocation(x)).collect()

    data = alarmCalc(2 * 100, 5, data)
    data = alarmCalc(2 * 1000, 20, data)
    data = alarmCalc(2 * 10000, 1000, data)
    data = alarmSort(data)

    for i in data:
        print(i)

    # data = sc.textFile(sys.argv[1], 1).collect()
    # for i in data:
    #     location = tuple(i.split(", "))
    #     print(location[14])
    #     print(location[15][:-1])

    sc.stop()

    # file = open("data.txt", "a+", encoding='utf-16')
    #
    # conn = pymssql.connect(host="10.50.100.52", user="sa", password="123456", database="GPS")
    # cursor = conn.cursor()
    #
    # start = 17015625
    # end = start + 10000
    #
    # sql = "SELECT * FROM GPSOverSpeed WHERE RecordId > %d AND RecordId <= %d"
    # param = (start, end)
    # cursor.execute(sql, param)
    # row = cursor.fetchall()
    #
    # while len(row) > 0:
    #     for i in row:
    #         file.write(i.__str__())
    #         file.write('\n')
    #     start += 10000
    #     end += 10000
    #     paramTemp = (start, end)
    #     cursor.execute(sql, paramTemp)
    #     row = cursor.fetchall()
    #     print("start...%d", start)
    #
    # print("end...")

    # db = pymysql.connect("10.50.40.145", "root", "cdwk-3g-145", "lv")
    # cursor = db.cursor()
    # cursor.execute("select * from mybatis_plus")
    # data = cursor.fetchall()
    # db.close()

    # for i in row:
    #     print(i)

