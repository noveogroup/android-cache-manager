Android Cache Manager
=====================

TODO: write a description

__Android SDK Version__: API 7 [ Android 2.1 ]

__Code Samples__: [[here]](https://github.com/noveogroup/android-cache-manager/tree/sample)

Downloads
---------

 - [android-cache-manager-1.3.4.jar](https://github.com/noveogroup/android-cache-manager/blob/gh-downloads/android-cache-manager-1.3.4.jar?raw=true)
 - [android-cache-manager-1.3.4-sources.jar](https://github.com/noveogroup/android-cache-manager/blob/gh-downloads/android-cache-manager-1.3.4-sources.jar?raw=true)
 - [android-cache-manager-1.3.4-javadoc.jar](https://github.com/noveogroup/android-cache-manager/blob/gh-downloads/android-cache-manager-1.3.4-javadoc.jar?raw=true)

[Previous versions](https://github.com/noveogroup/android-cache-manager/tree/gh-downloads)

Maven Dependency
----------------

    <dependency>
        <groupId>com.noveogroup.android</groupId>
        <artifactId>android-cache-manager</artifactId>
        <version>1.3.4</version>
    </dependency>

Gradle Dependency
-----------------

'com.noveogroup.android:android-cache-manager:1.3.4'


Getting Started
---------------

TODO: write Getting Started

Known Issues
============

1. ...

NoveoGroup Internal
-------------------

Here is NoveoGroup internal information.
Futher it will be translated but now it is placed in Russian.

```
TODO List
=========
1. Перенести CleanerHelper в пакет *.cleaner
2. Реализовать поддержку нескольких экземпляров DiskCache:
 1. Тогда нужна синхронизация настроек очистки, а также времени последней очистки.
 2. Дополнительное случайное смещение времени синхронизации, времени старта очистки.
 3. AbstractCleanerHelper в этом случае надо разделить - он будет слишком разный для разных реализаций кэша
 4. Пока разработки по синхронизации настроек находятся в отдельной ветке [[тут]](https://github.com/noveogroup/android-cache-manager/tree/sync-clean).
3. Подумать: может добавить в интерфейсы InputSource и OutputSource методы для проверки доступности соответственно чтения и записи ?

Часто задаваемые вопросы
========================
1. Какие типы данных могут храниться в кэше?
 * Временные файлы, лежащие в директории кэша. В частности, созданные методом createFile().
 * Entry. Включает в себя File, ключ и мета-данные.
2. Как что-либо положить в кэш?
 * Все операции вставки в кэш осуществляются методом put() по ключу.
 * Для удобства разработчика добавлен метод putMetaData() для добавления в кэш мета-данных.
3. Как что-либо достать из кэша?
 * Достать что-либо из кэша можно методом get(), указав соответствующий ключ.
 * Для удобства разработчиков добавлен метод getMetaData() для извлечения из кэша мета-данных.
4. Как очистить кэш?
 * Для полной очистки кэша служит метод erase(), который полностью очищает директорию кэша и возвращает его в начальное состояние.
 * Для запуска процедуры очистки кэша используется метод clean(), которая удаляет все элементы с "истекшими" maxAge и expirationTime, а также половину элементов с не истекшим expirationTime, которые выходят за границы размера кэша. Выполняется в отдельном потоке.
5. Как обновить файл в кэше?
 * Для обновления времени последней модификации файла лежащего в кэше используется метод touchFile().
6. Как задать максимальное время хранения в кэше?
 * Максимальное время хранения в кэше задается методом setMaxAge().
7. В чем отличия использования maxAge от expirationTime?
 * maxAge используется для указания времени максимально долгого хранения в кэше.
 * expirationTime указывает на рекомендуемое минимальное время хранения в кэше. Использование этого параметра не гарантирует того, что элемент не будет удален во время работы метода clean().
8. В чем отличие erase() от clean()?
 * erase() служит для полной очистки директории кэша и возврата кэша к начальному состоянию.
 * clean() удаляет все элементы с "истекшими" maxAge и expirationTime, а также половину элементов с не истекшим expirationTime, которые выходят за границы размера кэша. Выполняется в отдельном потоке.
9. Можно ли задавать директорию кэша?
 * Директория кэша может быть задана только при создании объекта класса. В последствии директорию сменить нельзя!
10. Какие настройки можно производить с кэшем?
 * setCleanAccessCount() позволяет задать число модификаций, после выполнения которых будет запускаться clean().
 * setCleanTimeDelay() позволяет задать интервал времени, через который будет запускаться clean().
 * setExpirationTime() позволяет задать рекомендуемое минимальное время хранения в кэше. Не гарантирует того, что элемент все это время будет храниться в кэше!
 * setMaxAge() позволяет задать максимально долгое время хранения в кэше.
 * setMaxSize() позволяет задать максимальный размер директории кэша, на который будет ориентироваться алгоритм метода clean().
11. Зачем нужны createFile() и createDirectory()?
 * createFile() позволяет создать новый временный файл в кэш-директории.
 * createDirectory() позволяет создать новую поддиректорию в кэш-директории.
12. Каковы возможные причины запуска процедуры очистки кэша?
 * Запрос пользователя.
 * Большое кол-во операций вставок.
 * Истечение тайм-аута.
13. Как хранить в кэше мета-данные?
 * Мета-данные можно хранить в кэше в качестве экземпляров объектов класса MetaData, который является наследником класса HashMap.
14. Можно ли отдельно задать время хранения в кэше (и другие параметры) конкретному элементу, отличающееся от времени хранения (и других параметров) прочих элементов?
 * Нет!
```

Developed By
============

* [Noveo Group][1]
* Pavel Stepanov - <pstepanov@noveogroup.com>

License
=======

    Copyright (c) 2013 Noveo Group

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    Except as contained in this notice, the name(s) of the above copyright holders
    shall not be used in advertising or otherwise to promote the sale, use or
    other dealings in this Software without prior written authorization.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
    THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.

[1]: http://noveogroup.com/
