import cv2

import matplotlib

import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle
from aicspylibczi import CziFile
import numpy as np
from sklearn.cluster import OPTICS
from tqdm import tqdm
from tkinter.ttk import Progressbar
from itertools import groupby
from operator import itemgetter
from sys import platform
import json
import os

class WormCluster1(object):

    def __init__(self, filename, opts, progress_bar=None, outfile='out.json'):
        self.filename = filename
        self.slices = []
        self.opts = opts
        self.output_n_points = None
        self.output_boxes = None
        self.cluster_data = []
        self.outfile=outfile

        if progress_bar is not None:
            self.progress_bar : Progressbar = progress_bar
        else:
            self.progress_bar = None

    def custom_compression(self, img_arr):
        compressed_arr = np.zeros((img_arr.shape[0] // self.opts['factor'], img_arr.shape[1] // self.opts['factor']), dtype=bool)

        for cur_y in range(img_arr.shape[1] // self.opts['factor']):
            for cur_x in range(img_arr.shape[0] // self.opts['factor']):
                if img_arr[cur_x * self.opts['factor']:(cur_x + 1) * self.opts['factor'], cur_y * self.opts['factor']:(cur_y + 1) * self.opts['factor']].mean() > self.opts['minimum']:
                    compressed_arr[cur_x, cur_y] = True

        return compressed_arr

    def cluster(self, img_arr, orig_shape, fig_name="temp.png", final=False):

        if not final:
            clust = OPTICS(min_samples=self.opts['min_samples'], xi=self.opts['xi'], n_jobs=-1)
        else:
            clust = OPTICS(min_samples=5, xi=0.05)
        # clust = OPTICS(min_samples=self.opts['min_samples'], cluster_method='xi', xi=0.23)

        # print('CLUSTERING {} POINTS...'.format(img_arr.shape[0]))
        try:
            clust.fit(img_arr)
        except ValueError:
            self.cluster_data.append([])
            return -1, [0, 0], [0, 0]

        idx = []
        for l in range(np.max(clust.labels_) + 1):
            idx.append([l, np.sum(clust.labels_ == l)])

        sorted_idx = sorted(idx, key=lambda x: x[1], reverse=True)

        def get_box(labels, group_num):
            x_range = [orig_shape[1], 0]
            y_range = [orig_shape[0], 1]
            for tup in img_arr[labels == group_num]:
                if tup[0] < x_range[0]:
                    x_range[0] = tup[0]
                if tup[0] > x_range[1]:
                    x_range[1] = tup[0]
                if tup[1] < y_range[0]:
                    y_range[0] = tup[1]
                if tup[1] > y_range[1]:
                    y_range[1] = tup[1]

            return x_range, y_range

        if final:
            use_label = 0

            if np.sum(clust.labels_ == use_label) == 0:
                self.cluster_data.append([])
                return -1, [0, 0], [0, 0]

            tbox_x, tbox_y = get_box(clust.labels_, sorted_idx[use_label][0])
        else:
            use_label = 1

            if np.sum(clust.labels_ == use_label) == 0:
                self.cluster_data.append([])
                return -1, [0, 0], [0, 0]

            tbox_x, tbox_y = get_box(clust.labels_, sorted_idx[use_label][0])
            if (tbox_x[1] - tbox_x[0]) * (tbox_y[1] - tbox_y[0]) > 1500:
                if np.sum(len(sorted_idx) > 2 and clust.labels_ == sorted_idx[2][0]) != 0:
                    use_label = 2
                    tbox_x, tbox_y = get_box(clust.labels_, sorted_idx[use_label][0])
                else:
                    return -1, [0, 0], [0, 0]

            self.cluster_data.append(img_arr[clust.labels_ == use_label])

        colors = ["g.", "r.", "b.", "y.", "c."]
        for klass, color in zip(range(0, 5), colors):
            Xk = img_arr[clust.labels_ == klass]
            plt.plot(Xk[:, 0], Xk[:, 1], color, alpha=0.3)
        plt.plot(img_arr[clust.labels_ == -1, 0], img_arr[clust.labels_ == -1, 1], "k+", alpha=0.1)
        plt.xlim([0, orig_shape[1]])
        plt.ylim([0, orig_shape[0]])
        plt.gca().invert_yaxis()
        ax = plt.gca()

        padding = 3
        rect = Rectangle((tbox_x[0] - padding, tbox_y[0] - padding), tbox_x[1] - tbox_x[0] + 2 * padding, tbox_y[1] - tbox_y[0] + 2 * padding, linewidth=1, edgecolor='b', facecolor='none')
        ax.add_patch(rect)

        # plt.show()
        if platform == 'win32':
            fname = os.path.join(self.outfile[:self.outfile.rfind('\\')], 'debug', self.filename[self.filename.rfind('\\')+1:] + '-' + fig_name)
        else:
            fname = os.path.join(self.outfile[:self.outfile.rfind('/')], 'debug', self.filename[self.filename.rfind('/')+1:] + '-' + fig_name)

        plt.savefig(fname)
        plt.clf()

        return np.sum(clust.labels_ == use_label), tbox_x, tbox_y

    def read_czi(self, path):
        czi = CziFile(self.filename)

        num_slices = czi.get_dims_shape()[0].get('Z')[1]
        self.output_n_points = np.zeros(num_slices)
        self.output_boxes = np.zeros((num_slices, 2, 2))

        for i in range(czi.get_dims_shape()[0].get('Z')[1]):
        # for i in [10,11,12,13]:
            img, shp = czi.read_image(Z=i)
            # image = Image.fromarray(img[0,0,0,0,0,:,:])
            # image.show()
            self.slices.append(img[0,0,0,0,0,:,:])

    def choose_range(self):
        total_groups = []
        for k, g in groupby(enumerate(list(np.arange(self.output_n_points.shape[0])[self.output_n_points > 0])), lambda i_x: i_x[0] - i_x[1]):
            total_groups.append(list(map(itemgetter(1), g)))

        def best_group(groups):
            res = []
            for i, g in enumerate([g for g in groups if len(g) > 2]):
                # calculate variance of number of points
                point_count_var = np.var(self.output_n_points[g])

                # calculate variance of box (0,0)
                box_origin_var = np.var(self.output_boxes[g][:, 0, 0])

                # calculate variance of box area
                areas = np.zeros(len(g))
                for i, box in enumerate(self.output_boxes[g]):
                    areas[i] = (box[0, 1] - box[0, 0]) * (box[1, 1] - box[1, 0])

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
        return preprocessed_groups[best_group(preprocessed_groups)]

    def save(self):

        try:
            with open(self.outfile, 'rb') as f:
                out_json = json.load(f)
        except:
            out_json = {'data': []}

        out_json['data'].append({
            'filename': self.filename,
            'slices': ','.join([str(c) for c in self.best_slices]),
            'box': ','.join([str(c) for c in self.final_box])
        })

        with open(self.outfile, 'w') as f:
            json.dump(out_json, f)

    def run(self):
        self.read_czi(self.filename)

        for i, slice in enumerate(tqdm(self.slices)):
            compressed_bw = self.custom_compression(slice)
            self.compressed_shape = compressed_bw.shape
            bw_img_idx = np.flip(np.transpose(compressed_bw.nonzero()))

            temp_res = self.cluster(bw_img_idx, compressed_bw.shape, '{}.png'.format(i))
            self.output_n_points[i] = temp_res[0]
            self.output_boxes[i,:,:] = temp_res[1:]

            if self.progress_bar is not None:
                self.progress_bar['value'] = (i+1) / len(self.slices) * 100

        if np.max(self.output_n_points) > 0:
            self.best_slices = self.choose_range()
            sorted_slices = sorted(zip(self.output_n_points[self.best_slices].tolist(), self.best_slices), key=lambda x: x[0])

            def adjust_box(box):
                box[0] -= 1
                box[1] += 1
                box[2] -= 1
                box[3] += 1

                box[0] *= self.opts['factor']
                box[1] *= self.opts['factor']
                box[2] *= self.opts['factor']
                box[3] *= self.opts['factor']

                return box

            self.opts['xi'] = 0.99
            final_cluster = self.cluster(self.cluster_data[sorted_slices[-1][1]], self.compressed_shape, 'final.png', final=True)
            self.final_box = adjust_box(final_cluster[1] + final_cluster[2])

        else:
            self.best_slices = []
            self.final_box = []

        self.save()

        # trunc_fname = self.filename[self.filename.rfind('/') + 1:]
        # np.save('/home/nathaniel/workspace/imagej/worm-cluster-1/python/debug/n_points-{}.npy'.format(trunc_fname), self.output_n_points)
        # np.save('/home/nathaniel/workspace/imagej/worm-cluster-1/python/debug/boxes-{}.npy'.format(trunc_fname), self.output_boxes)


if __name__ == '__main__':

    wcOpts = {
        'factor': 6,
        'minimum': 3,
        'min_samples': 20,
        'xi': 0
    }
    wc1 = WormCluster1('/home/nathaniel/workspace/imagej/worm-cluster-1/data/30.czi', wcOpts, outfile='out.json')
    wc1.run()

    # for i, tup in enumerate(wc1.output):
    #     print('{}:\t{}'.format(i,tup))

    # multiple_clusters = [9,10,11,12,13]
    # output = []
    # czi = CziFile("/home/nathaniel/workspace/imagej/worm-cluster-1/data/5.czi")

    # for i in tqdm(range(7,14)):
    # bw_img = cv2.imread('/home/nathaniel/workspace/imagej/worm-cluster-1/data/{}.jpg'.format(i), 0)
    #     img, shp = czi.read_image(Z=i)
    #     sliced_img = img[0,0,0,0,0,:,:]
    #     compressed_bw = custom_compression(sliced_img, 5, 5)
    #     output.append(cluster(bw_img_idx, compressed_bw.shape, '{}.png'.format(i)))
    # output.append(cluster(bw_img_idx, compressed_bw.shape))

        # if cluster(bw_img_idx, compressed_bw.shape) > 1:
        #     multiple_clusters.append(i)

    # [7, 8, 9, 10, 11, 12, 13, 51, 55, 67, 68, 69]
    #


