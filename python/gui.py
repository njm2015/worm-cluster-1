import tkinter
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


        try:
            shutil.rmtree(os.path.join(output_folder.get(),'debug'))
        except:
            pass

        os.mkdir(os.path.join(output_folder.get(), 'debug'))

        try:
            os.rename(os.path.join(output_folder.get(), 'output.json'), os.path.join(output_folder.get(), 'output-bak.json'))
        except:
            pass

        czis = []
        for file in os.listdir(data_folder.get()):
            if file[-3:] == 'czi':
                czis.append(os.path.join(data_folder.get(), file))

        for i,f in enumerate(czis):

            wc1_opts = {
                'factor': 6,
                'minimum': 3,
                'min_samples': 20,
                'xi': 0
            }

            wc1 = WormCluster1(f, wc1_opts, progress_bar=bar, outfile=os.path.join(output_folder.get(), 'output.json'))
            wc1.run(auto_analyze.get())

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

    auto_analyze = tkinter.BooleanVar()
    auto_analyze.set(True)
    auto_analyze_check_button = tkinter.Checkbutton(window, text="Auto Analyze", variable=auto_analyze, onvalue=True, offvalue=False)
    auto_analyze_check_button.grid(row=2, pady=20)

    start_btn = Button(window, text="Start", width=20, command=lambda: start_thread())
    start_btn.grid(columnspan=5, row=3, pady=10)

    bar = Progressbar(window, length=500)
    bar.grid(columnspan=5, row=4, pady=10)

    total_bar = Progressbar(window, length=500)
    total_bar.grid(columnspan=5, row=5, pady=10)


    def on_closing():
        if messagebox.askokcancel("Quit", "Do you want to quit?"):
            window.destroy()
            exit(0)


    window.protocol("WM_DELETE_WINDOW", on_closing)
    window.mainloop()