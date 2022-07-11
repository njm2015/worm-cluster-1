from tqdm import tqdm

if __name__ == '__main__':

    num1 = 2
    num2 = 2
    result = 0
    adder = 0.0000001

    # while result - num1 - num2 < 0:
    for i in tqdm(range(int(4 / adder))):
        result += adder

    print('Done! Result is {}'.format(result))