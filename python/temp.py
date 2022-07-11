import numpy as np
from itertools import groupby
from operator import itemgetter

if __name__ == '__main__':
    n_points = np.load('./debug/n_points-27.czi.npy')
    boxes = np.load('./debug/boxes-27.czi.npy')

    # print(n_points.shape)
    # print(np.arange(n_points.shape[0])[n_points > 0])

    total_groups = []
    for k,g in groupby(enumerate(list(np.arange(n_points.shape[0])[n_points > 0])), lambda i_x: i_x[0] - i_x[1]):
        total_groups.append(list(map(itemgetter(1), g)))

    def best_group(groups):
        res = []
        for i,g in enumerate([g for g in groups if len(g) > 2]):
            # calculate variance of number of points
            point_count_var = np.var(n_points[g])

            # calculate variance of box (0,0)
            box_origin_var = np.var(boxes[g][:,0,0])

            # calculate variance of box area
            areas = np.zeros(len(g))
            for i,box in enumerate(boxes[g]):
                areas[i] = (box[0,1] - box[0,0]) * (box[1,1] - box[1,0])

            res.append(point_count_var / 100 + box_origin_var + np.var(areas) / 1000)

        return np.argmin(res)

    def split_group(single_group, max_items):
        ret_groups = []

        for i in range(len(single_group) - max_items):
            ret_groups.append(single_group[i:i + max_items])

        return ret_groups

    def preprocess_groups(group_list):
        ret_list = []
        for g in group_list:
            if len(g) > 4:
                split_groups = split_group(g, 4)
                ret_list.append(split_groups[best_group(split_groups)])
            else:
                ret_list.append(g)

        return ret_list

    preprocessed_groups = preprocess_groups(total_groups)
    slices = preprocessed_groups[best_group(preprocessed_groups)]

    sorted_slices = sorted(zip(n_points[slices].tolist(), slices), key=lambda x: x[0])
    print(sorted_slices[-1][1])