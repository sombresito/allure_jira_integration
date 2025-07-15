package ru.iopump.qa.allure.service.generate;

import ru.iopump.qa.allure.service.generate.ErrorExecutingScript;
import java.util.*;

public class GenerateNameFl {

    public String GetNameFl(String sex, String res) throws ErrorExecutingScript, NullPointerException {
        // метод возвращает сгенерированную фамилию физ лица
        String name_fl = null;
        try {
            name_fl = GenNamingFl(sex, "Name", res);
        } catch (ErrorExecutingScript | NullPointerException except) {
            except.printStackTrace();
        }
        return name_fl;
    }

    public String GetSurnameFl(String sex, String res) throws ErrorExecutingScript, NullPointerException {
        // метод возвращает сгенерированное отчество/среднее имя физ лица
        String surname_fl = null;
        try {
            surname_fl = GenNamingFl(sex, "Surname", res);
        } catch (ErrorExecutingScript | NullPointerException except) {
            except.printStackTrace();
        }
        return surname_fl;
    }

    public String GetMiddleNameFl(String sex, String res) throws ErrorExecutingScript, NullPointerException {
        // метод возвращает сгенерированное имя физ лица
        String middlename_fl = null;
        try {
            middlename_fl = GenNamingFl(sex, "Middlename", res);
        } catch (ErrorExecutingScript | NullPointerException except) {
            except.printStackTrace();
        }
        return middlename_fl;
    }
    private String GenNamingFl(String sex, String type, String res) throws ErrorExecutingScript {
        // метод принимает пол, тип имени (имя, фамилия или отчество) и резиденстов (1 или 0)
        // и возвращает рандомное имя, фамилию или отчество
        Random rand = new Random();
        String get_naming;
        if (type == "Name") {
            if (res == "1") {
                if (sex == "M") {
                    List list_name_male = Arrays.asList("Аввакум", "Автоном", "Агафангел", "Агафоник", "Аггей", "Агриппин", "Акакий", "Алипий", "Ананий",
                            "Андроник", "Антилох", "Архелох", "Афиноген", "Африкан", "Боян", "Бронислав", "Брячислав",
                            "Будило", "Вукол", "Гвидон", "Гермоген", "Гонорат", "Досуг", "Евлампий", "Евпсихий",
                            "Епимах", "Зотик", "Казимир", "Каллистрат", "Кандид", "Капитон", "Кондрат", "Мокий",
                            "Нектарий", "Никазий", "Никодим", "Павсикакий", "Приск", "Садок", "Спиридон", "Феодул",
                            "Ферапонт", "Христодул", "Эгей", "Эразм", "Пропалаши", "Петрушка", "Факий", "Херман",
                            "Соссий", "Чобан", "Сидор", "Павсикакий", "Трифилий", "Харлампий", "Трактор", "Борзун",
                            "Ленинид", "Пофистал", "Питросий", "Распугай", "Бубон", "Бабай", "Вторак", "Блуд", "Север",
                            "Храбр", "Цицер", "Гедалий", "Бедрос", "Козловод", "Мандрагор", "Гапон", "Гандзя", "Тигрий",
                            "Спартак", "Динасий", "Галактион", "Мелитон", "Мокий", "Олегой", "Изяслав", "Добромысл",
                            "Карп", "Сидор", "Зоран", "Жигер", "Войтех", "Люсян", "Огнейслав", "Боян", "Боголеп");
                    int num_element = rand.nextInt(list_name_male.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_name_male.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else if (sex == "F") {
                    List list_name_female = Arrays.asList("Лалабелла", "Бамболина", "Зебралина", "Фантыжульетта", "Карамбулька", "Пуфлеттина",
                            "Галантышка", "Феерверкса", "Колобоксика", "Снежулинка", "Фантиксена", "Кокетта",
                            "Попандопалуська", "Аглая", "Глафира", "Евлампия", "Евпраксия", "Ефросиния",
                            "Епистима", "Ермиония", "Ия", "Марфа", "Милица", "Олимпиада", "Павлина",
                            "Пинна", "Рогнеда", "Улита", "Фёкла", "Есфирь", "Ненилка", "Бабура", "Богумила",
                            "Голуба", "Верхуслава", "Гостена", "Добронега", "Нелюба", "Томила", "Свтрона",
                            "Феврония", "Гея", "Кикилия", "Услада", "Нигина", "Вирсавия", "Яролика", "Богемия",
                            "Перизат", "Вевея", "Душица", "Ляля", "Поликсения", "Макрина", "Кончита", "Пердита",
                            "Пульхерия", "Феодулия", "Алгебрина", "Томила", "Ясна", "Васса", "Гелия", "Искра",
                            "Бухарина", "Жоресса", "Лента", "Магнита", "Томик", "Чилина", "Властелина", "Аблена",
                            "Бабина", "ЗавЕтушка", "Зазуля", "Обайда", "Сюр");
                    int num_element = rand.nextInt(list_name_female.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_name_female.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else {
                    throw new ErrorExecutingScript(String.format("Передан неверный тип пола - %s", sex));
                }
            } else if (res == "0") {
                if (Objects.equals(sex, "M")) {
                    List list_name_male_en = Arrays.asList("Aaron", "Adam", "Alan", "Albert", "Alex", "Alexander", "Alfred", "Andrew", "Andy",
                            "Anthony", "Arnold", "Arthur", "Barry", "Ben", "Benjamin", "Bernard", "Bill",
                            "Billy", "Bob", "Bobby", "Brad", "Brandon", "Brian", "Bruce", "Bryan", "Bud",
                            "Calvin", "Carl", "Carlos", "Charles", "Charlie", "Chris", "Christian", "Christopher",
                            "Colin", "Connie", "Curtis", "Dale", "Dan", "Daniel", "Danny", "Dave", "David",
                            "Davis", "Dean", "Dennis", "Derek", "Dick", "Don", "Donald", "Douglas", "Duke",
                            "Dustin", "Dylan", "Earl", "Edgar", "Edmond", "Edward", "Edwin", "Elton", "Emmett",
                            "Eric", "Ernest", "Ethan", "Felix", "Ferdinand", "Floyd", "Francis", "Frank", "Fred",
                            "Frederick", "Fuller", "Gary", "George", "Gerald", "Gilbert", "Glover", "Gordon",
                            "Graham", "Greg", "Harold", "Harrison", "Harry", "Henry", "Herbert", "Howard",
                            "Jack", "Jake", "James", "Jay", "Jeff", "Jerry", "Jim", "Joel", "John", "Johnny",
                            "Jon", "Jonathan", "Kane", "Keith", "Ken", "Kenneth", "Kevin", "Kurt", "Larry",
                            "Leo", "Leonard", "Louis", "Lynn", "Mark", "Martin", "Marvin", "Matt", "Maurice",
                            "Max", "Mel", "Melvin", "Michael", "Mike", "Nathan", "Neal", "Neil", "Nick", "Norman",
                            "Oliver", "Oscar", "Osteen", "Oswald", "Owen", "Patrick", "Paul", "Pete", "Peter", "Phil",
                            "Philip", "Ralph", "Randy", "Raymond", "Richard", "Rick", "Rob", "Robert", "Rodger",
                            "Roland", "Ronald", "Ronnie", "Roy", "Sam", "Samuel", "Sid", "Simon", "Smith", "Stephen",
                            "Steve", "Ted", "Terry", "Theodore", "Thomas", "Tim", "Tom", "Tony", "Victor", "Wayne",
                            "Wilhelm", "William", "Willie", "Wilson");
                    int num_element = rand.nextInt(list_name_male_en.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_name_male_en.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else if (Objects.equals(sex, "F")) {
                    List list_name_female_en = Arrays.asList("Abbe", "Abbey", "Abbie", "Abby", "Aberdeen", "Abigail", "Abney",
                            "Acy", "Ada", "Addie", "Addison", "Addy", "Adela", "Adelaide",
                            "Adele", "Adelle", "Adie", "Adriana", "Adrienne", "Afton", "Agatha",
                            "Aggie", "Aggy", "Agnes", "Aileen", "Aimee", "Airlie", "Alana", "Albany",
                            "Alberta", "Albertina", "Albertine", "Alene", "Alex", "Alexa", "Alexandra",
                            "Alexandria", "Alexis", "Alfreda", "Alfie", "Alfy", "Ali", "Alice",
                            "Alicia", "Aline", "Alisa", "Alison", "Alissa", "Alix", "Alla", "Alle",
                            "Allie", "Allin", "Allison", "Ally", "Allyson", "Alma", "Althea",
                            "Alton", "Alvena", "Alvina", "Alyce", "Alyson", "Alyssa", "Amabel",
                            "Amalia", "Amanda", "Amber", "Amelia", "Amie", "Amy", "Ana", "Anabel",
                            "Anastasia", "Andrea", "Andy", "Angel", "Angela", "Angelica", "Angelina",
                            "Angeline", "Angie", "Aniston", "Anita", "Annn", "Anna", "Annabel",
                            "Annabelle", "Anne", "Annetta", "Annette", "Annie", "Anthea", "Antoinette",
                            "Antonia", "Apple", "April", "Arabel", "Arabella", "Arabelle", "Bade", "Bailey",
                            "Baily", "Banner", "Baylor", "Beck", "Beckham", "Bee", "Bel", "Bell",
                            "Bella", "Bellamy", "Belle", "Bernett", "Berry", "Bersford", "Bertie",
                            "Bess", "Beth", "Beverley", "Beverly", "Bexley", "Billard", "Birdsong",
                            "Blakely", "Blakesley", "Blue", "Blythe", "Bonney", "Bramble", "Bray",
                            "Cake", "Candy", "Carman", "Carner", "Carrington", "Cartwright", "Case",
                            "Cass", "Cena", "Challen", "Chanel", "Chaney", "Channing", "Chapel", "Cheney",
                            "Cherry", "Chesney", "Cheyne", "Christmas", "Christy", "Clarke", "Cloud");
                    int num_element = rand.nextInt(list_name_female_en.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_name_female_en.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else {
                    throw new ErrorExecutingScript(String.format("Передан неверный тип пола - %s", sex));
                }
            } else {
                throw new ErrorExecutingScript(String.format("Передан неверный тип резиденства - %s", res));
            }
        } else if (type == "Surname") {
            if (res == "1") {
                if (sex == "M") {
                    List list_surname_male_ru = Arrays.asList("Аболдуев", "Абыхвост", "Абыяк", "Акулибаба", "Альфонсович", "Ананьев",
                            "Аннус", "Бабайда", "Бабанин", "Бабело", "Бабенец", "Балака", "Бананович",
                            "Бандюк", "Баран", "Барбос", "Безденежный", "Безобразов", "Бей", "Белодед",
                            "Беспарточный", "Бибик", "Билайнер", "Бита-Марья", "Блоха", "Блуд", "Блюдников",
                            "Блябкин", "Бляблин", "Бляденко", "Блядько", "Блятник", "Бляхер", "Бляхман",
                            "Бобик", "Бобинчик", "Бобрик", "Бовдур", "Болибок", "Братишко", "Брехунов",
                            "Бриллиантский", "Брысь", "Буйвол-Кот", "Бухоглот", "Бухайло", "Бухаров", "Бык",
                            "Вагина", "Вареник", "Ватник", "Верныволя", "Вздергивайлов", "Винфлинзидор",
                            "Вислоушкин", "Водка", "Волкодав", "Вшивца", "Выпирайлов", "Вырвикишко",
                            "Выставнога", "Выхрестюк", "Гаврик", "Гадючка", "Гайдамака", "Галимов",
                            "Ганган", "Гвиздало", "Глуходед", "Глюкин", "Гнида", "Гниломясов", "Гниляк",
                            "Голенький", "Голобурда", "Головатюк", "Голомудько", "Грибожор", "Гробокопатель",
                            "Грызидуб", "Грязноруков", "Гуляйдень", "Давикоза", "Дебелый", "Дебилле",
                            "Деньгубов", "Дереза", "Дериглазов", "Дидусь", "Дикий", "Добробаба", "Добрыдень",
                            "Долбня", "Дохлик", "Драндар", "Дратва", "Дрищ", "Дрочек", "Дрюкач", "Дрюченко");
                    int num_element = rand.nextInt(list_surname_male_ru.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_surname_male_ru.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else if (sex == "F") {
                    List list_surname_female_ru = Arrays.asList("Аболдуева", "Абыхвост", "Абыяк", "Акулибаба", "Альфонсович", "Ананьева",
                            "Аннус", "Бабайда", "Бабанина", "Бабело", "Бабенец", "Балака", "Бананович",
                            "Бандюк", "Баран", "Барбос", "Безденежная", "Безобразова", "Бей", "Белодед",
                            "Беспарточная", "Бибик", "Билайнер", "Бита-Марья", "Блоха", "Блуд", "Блюдникова",
                            "Блябкина", "Бляблина", "Бляденко", "Блядько", "Блятник", "Бляхер", "Бляхман",
                            "Бобик", "Бобинчик", "Бобрик", "Бовдур", "Болибок", "Братишко", "Брехунова",
                            "Бриллиантский", "Брысь", "Буйвол-Кот", "Бухоглот", "Бухайло", "Бухарова", "Бык",
                            "Вагина", "Вареник", "Ватник", "Верныволя", "Вздергивайлова", "Винфлинзидор",
                            "Вислоушкина", "Водка", "Волкодав", "Вшивца", "Выпирайлова", "Вырвикишко",
                            "Выставнога", "Выхрестюк", "Гаврик", "Гадючка", "Гайдамака", "Галимова",
                            "Ганган", "Гвиздало", "Глуходед", "Глюкина", "Гнида", "Гниломясова", "Гниляк",
                            "Голенькая", "Голобурда", "Головатюк", "Голомудько", "Грибожор", "Гробокопатель",
                            "Грызидуб", "Грязнорукова", "Гуляйдень", "Давикоза", "Дебелая", "Дебилле",
                            "Деньгубова", "Дереза", "Дериглазова", "Дидусь", "Дикий", "Добробаба", "Добрыдень",
                            "Долбня", "Дохлик", "Драндар", "Дратва", "Дрищ", "Дрочек", "Дрюкач", "Дрюченко");
                    int num_element = rand.nextInt(list_surname_female_ru.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_surname_female_ru.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else {
                    throw new ErrorExecutingScript(String.format("Передан неверный тип пола - %s", sex));
                }
            } else if (res == "0") {
                if (Objects.equals(sex, "M")) {
                    List list_surname_male_en = Arrays.asList("Abramson", "Adamson", "Adderiy", "Addington", "Adrian", "Albertson", "Aldridge",
                            "Alford", "Allsopp", "Anderson", "Andrews", "Archibald", "Arnold", "Arthurs",
                            "Atcheson", "Attwood", "Audley", "Austin", "Ayrton", "Babcock", "Backer", "Baldwin",
                            "Bargeman", "Barlow", "Barnes", "Barrington", "Baskerville", "Batchelor", "Bates",
                            "Bawerman", "Beam", "Beecham", "Becker", "Benson", "Bethell", "Berrington", "Bidder",
                            "Birch", "Bishop", "Black", "Blair", "Bloomfield", "Bolman", "Boothby", "Bootman",
                            "Bosworth", "Bott", "Botterill", "Boyer", "Bradberry", "Bradshaw", "Bragg", "Brewster",
                            "Brickman", "Broderick", "Bromfield", "Brown", "Bruce", "Bush", "Calhoun", "Colquhoun",
                            "Campbell", "Campion", "Cantwell", "Carey", "Carrington", "Carroll", "Charles", "Carter",
                            "Chandler", "Chapman", "Charlson", "Chesterton", "Clapton", "Clarkson", "Cleland", "Clifford",
                            "Cloud", "Clowes", "Close", "Coats", "Cockerill", "Coleman", "Collingwood", "Connors",
                            "Conchobar", "Cook", "Copeland", "Coupland", "Cowpland", "Copland", "Cosgrove", "Cowell",
                            "Cramer", "Creighton", "Crockett", "Croftoon", "Crosby", "Crosbie", "Crosman", "Culver",
                            "Cushing");
                    int num_element = rand.nextInt(list_surname_male_en.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_surname_male_en.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else if (Objects.equals(sex, "F")) {
                    List list_surname_female_en = Arrays.asList("Abramson", "Adamson", "Adderiy", "Addington", "Adrian", "Albertson", "Aldridge",
                            "Alford", "Allsopp", "Anderson", "Andrews", "Archibald", "Arnold", "Arthurs",
                            "Atcheson", "Attwood", "Audley", "Austin", "Ayrton", "Babcock", "Backer", "Baldwin",
                            "Bargeman", "Barlow", "Barnes", "Barrington", "Baskerville", "Batchelor", "Bates",
                            "Bawerman", "Beam", "Beecham", "Becker", "Benson", "Bethell", "Berrington", "Bidder",
                            "Birch", "Bishop", "Black", "Blair", "Bloomfield", "Bolman", "Boothby", "Bootman",
                            "Bosworth", "Bott", "Botterill", "Boyer", "Bradberry", "Bradshaw", "Bragg", "Brewster",
                            "Brickman", "Broderick", "Bromfield", "Brown", "Bruce", "Bush", "Calhoun", "Colquhoun",
                            "Campbell", "Campion", "Cantwell", "Carey", "Carrington", "Carroll", "Charles", "Carter",
                            "Chandler", "Chapman", "Charlson", "Chesterton", "Clapton", "Clarkson", "Cleland", "Clifford",
                            "Cloud", "Clowes", "Close", "Coats", "Cockerill", "Coleman", "Collingwood", "Connors",
                            "Conchobar", "Cook", "Copeland", "Coupland", "Cowpland", "Copland", "Cosgrove", "Cowell",
                            "Cramer", "Creighton", "Crockett", "Croftoon", "Crosby", "Crosbie", "Crosman", "Culver",
                            "Cushing");
                    int num_element = rand.nextInt(list_surname_female_en.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_surname_female_en.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else {
                    throw new ErrorExecutingScript(String.format("Передан неверный тип пола - %s", sex));
                }
            } else {
                throw new ErrorExecutingScript(String.format("Передан неверный тип резиденства - %s", res));
            }
        } else if (type == "Middlename") {
            if (res == "1") {
                if (sex == "M") {
                    List list_middlename_male_ru = Arrays.asList("Александрович", "Андреевич", "Даниилович", "Феликсович", "Федорович", "Ильич",
                            "Иванович", "Иванович", "Максимилианович", "Михайлович", "Николаевич", "Сергеевич",
                            "Степанович", "Тимурович", "Вадимович", "Васильевич", "Владимирович", "Вольфович",
                            "Вячеславович", "Ярославич", "Алексеевич", "Авенирович", "Давидович", "Дмитриевич",
                            "Евгеньевич", "Генрихович", "Григорьевич", "Игнатьевич", "Хрисанфович", "Константинович",
                            "Леонидович", "Матвеевич", "Павлович", "Порфирьевич", "Саулович", "Соломонович",
                            "Тимофеевич", "Валентинович", "Васильевич", "Вульфович", "Егорович", "Абрамович",
                            "Афанасьевич", "Ахатович", "Андреевич", "Аркадьевич", "Донатович", "Феофилактович",
                            "Гаврилович", "Петрович", "Петрович", "Торекулович", "Юрьевич", "Зиновьевич",
                            "Альбертович", "Аниславович", "Антонович", "Арсеньевич", "Богданович", "Борисович",
                            "Феликсович", "Гарриевич", "Георгиевич", "Игоревич", "Иосифович", "Леонидович");
                    int num_element = rand.nextInt(list_middlename_male_ru.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_middlename_male_ru.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else if (sex == "F") {
                    List list_middlename_female_ru = Arrays.asList("Александровна", "Андреевна", "Данииловна", "Феликсовна", "Федоровна", "Ильична",
                            "Ивановна", "Ивановна", "Максимилиановна", "Михайловна", "Николаевна", "Сергеевна",
                            "Степановна", "Тимуровна", "Вадимовна", "Васильевна", "Владимировна", "Вольфовна",
                            "Вячеславовна", "Ярославна", "Алексеевна", "Авенировна", "Давидовна", "Дмитриевна",
                            "Евгеньевна", "Генриховна", "Григорьевна", "Игнатьевна", "Хрисанфовна", "Константиновна",
                            "Леонидовна", "Матвеевна", "Павловна", "Порфирьевна", "Сауловна", "Соломоновна",
                            "Тимофеевна", "Валентиновна", "Васильевна", "Вульфовна", "Егоровна", "Абрамовна",
                            "Афанасьевна", "Ахатовна", "Андреевна", "Аркадьевна", "Донатовна", "Феофилактовна",
                            "Гавриловна", "Петровна", "Петровна", "Торекуловна", "Юрьевна", "Зиновьевна",
                            "Альбертовна", "Аниславовна", "Антоновна", "Арсеньевна", "Богдановна", "Борисовна",
                            "Феликсовна", "Гарриевна", "Георгиевна", "Игоревна", "Иосифовна", "Леонидовна");
                    int num_element = rand.nextInt(list_middlename_female_ru.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_middlename_female_ru.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else {
                    throw new ErrorExecutingScript(String.format("Передан неверный тип пола - %s", sex));
                }
            } else if (res == "0") {
                if (Objects.equals(sex, "M")) {
                    List list_middlename_male_en = Arrays.asList("Adam", "Alan", "Alexander", "Andrew", "Anthony", "Arthur",
                            "Benjamin", "Bradley", "Carter", "Charles", "Christopher", "Cole",
                            "Cooper", "Daniel", "David", "Edward", "George", "Graham",
                            "Gregory", "Harry", "Henry", "Jack", "Jackson", "James",
                            "John", "Jonathan", "Joseph", "King", "Louis", "Matthew",
                            "Michael", "Nathaniel", "Oliver", "Peter", "Philip", "Richard",
                            "Robert", "Scott", "Thomas", "William");
                    int num_element = rand.nextInt(list_middlename_male_en.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_middlename_male_en.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else if (Objects.equals(sex, "F")) {
                    List list_middlename_female_en = Arrays.asList("Abigail", "Alice", "Amy", "Anne", "Ava", "Belle",
                            "Caroline", "Catherine", "Charlotte", "Claire", "Danielle", "Elizabeth",
                            "Eve", "Faith", "Faye", "Grace", "Hope", "Isabella",
                            "Isobel", "Ivy", "Jade", "Jane", "Jean", "Joy",
                            "June", "Kate", "Lily", "Louisa", "Louise", "Lynn",
                            "Mae", "Margaret", "Marie", "Mary", "May", "Michelle",
                            "Nicole", "Olivia", "Paige", "Renee", "Rose", "Ruby",
                            "Sophia", "Sophie", "Susanne", "Victoria");
                    int num_element = rand.nextInt(list_middlename_female_en.size()); // получаем рандомное число от 0 до максимального размера списка имен
                    get_naming = (String) list_middlename_female_en.get(num_element); // По сгенерированному индексу получаем элемент по индексу
                } else {
                    throw new ErrorExecutingScript(String.format("Передан неверный тип пола - %s", sex));
                }
            } else {
                throw new ErrorExecutingScript(String.format("Передан неверный тип резиденства - %s", res));
            }
        } else {
            throw new ErrorExecutingScript(String.format("Передан неверный тип нейминга - %s", type));
        }
        return get_naming;
    }
}

