from tkinter import *
from tkinter.filedialog import *
from tkinter.ttk import Progressbar
from cluster import WormCluster1
from threading import Thread
from tkinter import messagebox
import os
import shutil

t1 : Thread = None

if __name__ == '__main__':

    def choose_data_folder():
        dir = askdirectory()
        data_folder.delete(0, END)
        data_folder.insert(0, dir)

    def choose_output_folder():
        dir = askdirectory()
        output_folder.delete(0, END)
        output_folder.insert(0, dir)

    def start_thread():
        t1 = Thread(target=start)
        t1.start()

    def start():
        wc1_opts = {
            'factor': 6,
            'minimum': 3,
            'min_samples': 20,
            'xi': 0
        }

        shutil.rmtree(output_folder.get() + '/debug')
        os.mkdir(output_folder.get() + '/debug')
        os.rename(output_folder.get() + '/output.json', output_folder.get() + '/output-bak.json')

        czis = []
        for file in os.listdir(data_folder.get()):
            if file[-3:] == 'czi':
                czis.append(data_folder.get() + '/' + file)

        for i,f in enumerate(czis):
            wc1 = WormCluster1(f, wc1_opts, progress_bar=bar, outfile=output_folder.get() + '/output.json')
            wc1.run()

            total_bar['value'] = (i+1) / len(czis) * 100



    window = Tk()

    window.title("ROI Identification Batch Job")
    window.geometry('600x300')

    window.columnconfigure(0, weight=2)
    window.columnconfigure(1, weight=3)
    window.columnconfigure(2, weight=1)

    data_folder_lbl = Label(window, text="Data Folder Location")
    data_folder_lbl.grid(column=0, row=0, pady=10)

    data_folder = Entry(window, width=40)
    data_folder.grid(column=1, row=0, pady=10)

    data_folder_btn = Button(window, text="Choose", command=lambda: choose_data_folder())
    data_folder_btn.grid(column=2, row=0, pady=10)

    output_folder_lbl = Label(window, text="Output Directory")
    output_folder_lbl.grid(column=0, row=1)

    output_folder = Entry(window, width=40)
    output_folder.grid(column=1, row=1)

    output_folder_btn = Button(window, text="Choose", command=lambda: choose_output_folder())
    output_folder_btn.grid(column=2, row=1)

    start_btn = Button(window, text="Start", width=20, command=lambda: start_thread())
    start_btn.grid(columnspan=5, row=2, pady=10)

    bar = Progressbar(window, length=500)
    bar.grid(columnspan=5, row=3, pady=10)

    total_bar = Progressbar(window, length=500)
    total_bar.grid(columnspan=5, row=4, pady=10)


    def on_closing():
        if messagebox.askokcancel("Quit", "Do you want to quit?"):
            window.destroy()
            exit(0)


    window.protocol("WM_DELETE_WINDOW", on_closing)
    window.mainloop()