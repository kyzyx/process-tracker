class RingBuffer:
    def __init__(self, size=10):
        self._size = size
        self._data = []
        self._endptr = 0

    def _inc(self, i):
        return (i+1)%self._size

    def push(self, o):
        try:
            self._data[self._endptr] = o
        except:
            self._data.append(o)
        self._endptr = self._inc(self._endptr)

    def __iter__(self):
        curr = 0 if len(self._data) < self._size else self._endptr
        while curr < len(self._data):
            yield self._data[curr]
            curr = self._inc(curr)
            if curr == self._endptr:
                break
        raise StopIteration

